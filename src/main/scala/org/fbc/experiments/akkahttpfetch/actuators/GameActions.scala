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
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Cookie, HttpCookiePair}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging
import org.fbc.experiments.akkahttpfetch.extractors.ActiveGameListExtractor.getXML
import org.fbc.experiments.akkahttpfetch.model._

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class GameActions

object GameActions extends StrictLogging {
  private val moveUri = "http://www.boiteajeux.net/jeux/tza/traitement.php?id=%s"
  private val newGameUri = "http://www.boiteajeux.net/gestion.php"

  private val FROM_ACTION = "choisirSource"
  private val TO_ACTION = "destination"
  private val PASS_ACTION = "passer"

  def startNewGame(login: String, password: String, invitation: GameInvitation)
                  (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Future[String]
  = {
    logger.info(s"startNewGame $login, password, $invitation")
    val result = for {
      cookies <- WebFetcher.loginPost(login, password)
      response <- startNewGamePost(cookies, invitation)
      doc <- responseFutureToDoc(response)
      gameId <- interpreteNewGameResponse(doc)
    } yield gameId
    result
  }

  private def responseFutureToDoc(response: HttpResponse)
                                 (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer)

  : Future[String] = {
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

  private def postForm(uri: String, form: Map[String, String], cookies: Seq[HttpCookiePair])
                      (implicit system: ActorSystem, materializer: ActorMaterializer)
  = {
    val entity = akka.http.scaladsl.model.FormData(form).toEntity(HttpCharsets.`UTF-8`)
    val headers: Seq[HttpHeader] = if (cookies.isEmpty) Seq.empty else Seq(Cookie(cookies))
    val request = HttpRequest(uri = uri, method = HttpMethods.POST, entity = entity, headers = headers)
    Http().singleRequest(request)
  }

  private def interpreteNewGameResponse(doc: String)
                               (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer)
  : Future[String]
  = {
    logger.info("interpreteNewGameResponse")

    val docXml: Elem = getXML(doc)
    // BAJ doesn't validate input parameters and answers with "game created" - unless you are not logged in
    // (for example, you could give pTypePlateau=17 and the game gets created, with an empty board)
    val gameId = extractInviteId((docXml \ "strong").text.trim)
    println(s"----- ${gameId}")
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

  def joinGame(cookies: Seq[HttpCookiePair], login: String, password: String) = ???

//
//  // In every move-making stage there is a form submitted:
//  // <form name="fmPlateau" method="post" action="traitement.php?id=37682">
//  //    <input type="hidden" name="pAction" value="">
//  //    <input type="hidden" name="pL" value="">
//  //    <input type="hidden" name="pC" value="">
//  //    <input type="hidden" name="pIdCoup" value="598413fbb7dfb">
//  //	<input type="button" class="clBouton" value="PASS" "="" onclick="faire('passer',0,0)">
//  //  </form>
//  // Move stages (pAction) are:
//  // * choose source (`choisirSource`)-> `destination` / `annuler`, or
//  // * pass (`passer`)
//  // The only "strange" thing here is `pIdCoup` parameter. (It's probably some kind of timestamp or turn "marker".)
//  // I am not sure if it is required. For beginning, I'll send it exactly as original BAJ page does
//  def makeMove(cookies: Seq[HttpCookiePair], gameId: String, fullMove: FullMove) = {
//    logger.info("makeMove {}", fullMove)
//
//    require(fullMove.firstMove.moveType != PASS, "You cannot PASS on first move")
//    require(fullMove.firstMove.moveType == CAPTURE, "First move must be capture")
//
//    makeCaptureOrStackMove(browser, gameId, fullMove.firstMove)
//    fullMove.secondMove.moveType match {
//      case PASS => makePassMove(browser, gameId)
//      case _ => makeCaptureOrStackMove(browser, gameId, fullMove.secondMove)
//    }
//  }
//
//  def makePassMove(cookies: Seq[HttpCookiePair], gameId: String) = {
//    logger.info("makePassMove")
//    val mark = GameDetailsExtractor.extractTurnMarker(WebFetcher.getGameDetailsDoc(browser, gameId))
//    val doc = postPassAction(browser, mark, gameId)
//  }
//
//  private def postPassAction(cookies: Seq[HttpCookiePair], mark: String, gameId: String) = {
//    logger.info("post move form with pass action")
//    val moveForm = Map("pAction" -> PASS_ACTION, "pL" -> "", "pC" -> "", "pIdCoup" -> mark)
//    browser.post(String.format(moveUri, gameId), moveForm)
//  }
//
//  def makeCaptureOrStackMove(cookies: Seq[HttpCookiePair], gameId: String, move: Move) = {
//    require(move.from.nonEmpty && move.to.nonEmpty, "Capture or stack move needs `from` and `to`")
//
//    // get game page and extract pIdCoup
//    var mark = GameDetailsExtractor.extractTurnMarker(WebFetcher.getGameDetailsDoc(browser, gameId))
//
//    // send first half move (and read new pIdCoup)
//    var doc = postMoveAction(browser, mark, gameId, FROM_ACTION, move.from.get)
//    //    logger.info("--------------- dumping response html")
//    //    dumpToFile("response_1.html", doc.toHtml)
//    mark = GameDetailsExtractor.extractTurnMarker(doc)
//
//    // send second half move
//    doc = postMoveAction(browser, mark, gameId, TO_ACTION, move.to.get)
//    mark = GameDetailsExtractor.extractTurnMarker(doc)
//  }
//
//  private def postMoveAction(cookies: Seq[HttpCookiePair], mark: String, gameId: String, action: String, position: String) = {
//    logger.info("post move form {}, {}", action, position)
//    val (row, column) = new BajBoard().toPhysicalCoordinates(position)
//    logger.info("column and row for {} are {}", position, (column, row))
//    val moveForm = Map("pAction" -> action, "pL" -> row.toString, "pC" -> column.toString, "pIdCoup" -> mark)
//    browser.post(String.format(moveUri, gameId), moveForm)
//  }
//
}