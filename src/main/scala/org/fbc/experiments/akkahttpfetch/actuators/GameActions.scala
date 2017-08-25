/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.fbc.experiments.akkahttpfetch.actuators

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Cookie, HttpCookiePair}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging
import org.fbc.experiments.akkahttpfetch.extractors.ActiveGameListExtractor.getXML
import org.fbc.experiments.akkahttpfetch.extractors.GameDetailsExtractor
import org.fbc.experiments.akkahttpfetch.model._
import org.fbc.experiments.akkahttpfetch.utils.ProxyTools

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class GameActions

object GameActions extends StrictLogging with ProxyTools {
  private val moveUri = "http://www.boiteajeux.net/jeux/tza/traitement.php?id=%s"
  private val newGameUri = "http://www.boiteajeux.net/gestion.php"

  private val FROM_ACTION = "choisirSource"
  private val TO_ACTION = "destination"
  private val PASS_ACTION = "passer"

  def startNewGame(cookies: Seq[HttpCookiePair], invitation: GameInvitation)
                  (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Future[String] = {
    logger.info(s"startNewGame with cookies $invitation")
    val result = for {
      response <- startNewGamePost(cookies, invitation)
      doc <- responseToDoc(response)
      gameId <- interpreteNewGameResponse(doc)
    } yield gameId
    result
  }

  private def responseToDoc(response: HttpResponse)(implicit ec: ExecutionContext, materializer: ActorMaterializer): Future[String] = {
    Unmarshal(response.entity).to[String]
  }

  private def startNewGamePost(cookies: Seq[HttpCookiePair], invitation: GameInvitation)
                              (implicit system: ActorSystem, materializer: ActorMaterializer)
  : Future[HttpResponse] = {
    val form = Map(
      "pAction" -> "creer",
      "pJeu" -> "tza",
      "pNomPartie" -> invitation.gameName,
      "pEloMin" -> invitation.eloFrom.getOrElse(""),
      "pEloMax" -> invitation.eloTo.getOrElse(""),
      "pInvite%5B%5D" -> invitation.invitedPlayer.getOrElse(""),
      "pTypePlateau" -> (if (invitation.setup == SYMMETRICAL) "0" else "1")
    )
    postForm(newGameUri, form, cookies)
  }

  def interpreteNewGameResponse(doc: String)(implicit ec: ExecutionContext) : Future[String] = {
    logger.info("interpreteNewGameResponse")
    val docXml: Elem = getXML(doc)
    // BAJ doesn't validate input parameters and answers with "game created" - unless you are not logged in
    // (for example, you could give pTypePlateau=17 and the game gets created, with an empty board)
    val gameId = extractInviteId((docXml \\ "strong").text.trim)
    Future {
      gameId match {
        case Right(gameId) => gameId
        case Left(msg) => throw new IllegalStateException(msg)
      }
    }
  }

  private def extractInviteId(text: String): Either[String, String] = {
    val regexPattern = """game (\d+).*""".r
      text match {
        case regexPattern(gameId) => Right(gameId)
        case _ => Left(text)
      }
  }

  def joinGame(cookies: Seq[HttpCookiePair], gameId: String)
              (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer)
  : Future[String] = {
    logger.info(s"joinGame $gameId")
    val result = for {
      response <- joinGamePost(cookies, gameId)
      // joinGame in BAJ returns 302 (no real answer) so there is no chance to check if the operation was successful
      // probably we'll need to check if the accepted gameId showed in active game list
      doc <- responseToDoc(response)
    } yield doc
    result
  }

  // In every move-making stage there is a form submitted:
  // <form name="fmPlateau" method="post" action="traitement.php?id=37682">
  //    <input type="hidden" name="pAction" value="">
  //    <input type="hidden" name="pL" value="">
  //    <input type="hidden" name="pC" value="">
  //    <input type="hidden" name="pIdCoup" value="598413fbb7dfb">
  //  <input type="button" class="clBouton" value="PASS" "="" onclick="faire('passer',0,0)">
  //  </form>
  // Move stages (pAction) are:
  // * choose source (`choisirSource`)-> `destination` / `annuler`, or
  // * pass (`passer`)
  // The only "strange" thing here is `pIdCoup` parameter. (It's probably some kind of timestamp or turn "marker".)
  // I am not sure if it is required. For beginning, I'll send it exactly as original BAJ page does
  def makeMove(cookies: Seq[HttpCookiePair], gameId: String, fullMove: FullMove)
              (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Future[String] = {
    logger.info("makeMove {}", fullMove)

    require(fullMove.firstMove.moveType != PASS, "You cannot PASS on first move")
    require(fullMove.firstMove.moveType == CAPTURE, "First move must be capture")

    for {
      resp1 <- makeCaptureOrStackMove(cookies, gameId, fullMove.firstMove)
      // possibility to use turn marker from the first move (now below code rereads the details page for this)
      resp2 <- fullMove.secondMove.moveType match {
        case PASS => makePassMove (cookies, gameId)
        case _ => makeCaptureOrStackMove (cookies, gameId, fullMove.secondMove)
      }
    } yield resp2
  }

  private def makePassMove(cookies: Seq[HttpCookiePair], gameId: String)
                  (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Future[String] = {
    logger.info("makePassMove")
    for {
      doc <- WebFetcher.getGameDetailsDoc(cookies, gameId)
      resp <- postPassAction(cookies, GameDetailsExtractor.extractTurnMarker(doc), gameId)
      doc1 <- responseToDoc(resp)
    } yield GameDetailsExtractor.extractTurnMarker(doc1)
  }

  private def makeCaptureOrStackMove(cookies: Seq[HttpCookiePair], gameId: String, move: Move)
                            (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Future[String] = {
    logger.info("makeCaptureOrStackMove {}", move)
    require(move.from.nonEmpty && move.to.nonEmpty, "Capture or stack move needs `from` and `to`")

    for {
      doc <- WebFetcher.getGameDetailsDoc(cookies, gameId)
      _ <- postMoveAction(cookies, GameDetailsExtractor.extractTurnMarker(doc), gameId, FROM_ACTION, move.from.get)
      // if akka-http client would follow redirect, we wouldn't need to refetch game details page (we would just analyze response from above):
      doc1 <- WebFetcher.getGameDetailsDoc(cookies, gameId)
      _ <- postMoveAction(cookies, GameDetailsExtractor.extractTurnMarker(doc1), gameId, TO_ACTION, move.to.get)
      doc2 <- WebFetcher.getGameDetailsDoc(cookies, gameId)
    } yield GameDetailsExtractor.extractTurnMarker(doc2)
  }

  private def postPassAction(cookies: Seq[HttpCookiePair], mark: String, gameId: String)
                            (implicit system: ActorSystem, materializer: ActorMaterializer): Future[HttpResponse] = {
    logger.debug("post move form with pass action")
    val form = Map("pAction" -> PASS_ACTION, "pL" -> "", "pC" -> "", "pIdCoup" -> mark)
    postForm(String.format(moveUri, gameId), form, cookies)
  }

  private def postMoveAction(cookies: Seq[HttpCookiePair], mark: String, gameId: String, action: String, position: String)
                            (implicit system: ActorSystem, materializer: ActorMaterializer): Future[HttpResponse] = {
    logger.debug("post move form; mark: {}, action: {}, position: {}", mark, action, position)
    val (row, column) = new BajBoard().toPhysicalCoordinates(position)
    val moveForm = Map("pAction" -> action, "pL" -> row.toString, "pC" -> column.toString, "pIdCoup" -> mark)
    postForm(String.format(moveUri, gameId), moveForm, cookies)
  }

  private def joinGamePost(cookies: Seq[HttpCookiePair], gameId: String)(implicit system: ActorSystem, materializer: ActorMaterializer)
  : Future[HttpResponse] = {
    val form = Map(
      "pAction" -> "rejoindre",
      "inv" -> "i",
      "id" -> s"tza-$gameId"
    )
    postForm(newGameUri, form, cookies)
  }

  private def postForm(uri: String, form: Map[String, String], cookies: Seq[HttpCookiePair])
                      (implicit system: ActorSystem, materializer: ActorMaterializer): Future[HttpResponse]  = {
    val entity = akka.http.scaladsl.model.FormData(form).toEntity(HttpCharsets.`UTF-8`)
    val headers: Seq[HttpHeader] = if (cookies.isEmpty) Seq.empty else Seq(Cookie(cookies))
    val request = HttpRequest(uri = uri, method = HttpMethods.POST, entity = entity, headers = headers)
    singleRequestWithProxy(request)
  }
}
