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

package org.fbc.experiments.akkahttpfetch.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging
import org.fbc.experiments.akkahttpfetch.actuators.{GameActions, WebFetcher}
import org.fbc.experiments.akkahttpfetch.extractors.{ActiveGameListExtractor, GameDetailsExtractor}
import org.fbc.experiments.akkahttpfetch.model._

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

trait GameApi extends StrictLogging {

  def createGame(login: String, password: String, invitation: GameInvitation)
                (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Future[String] = {
    logger.info(s"startNewGame $login, password, $invitation")
    for {
      cookies <- WebFetcher.loginPost(login, password)
      gameId <- GameActions.startNewGame(cookies, invitation)
    } yield gameId
  }

  def joinGame(login: String, password: String, gameId: String)
              (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer) : Future[String] = {
    logger.info(s"joinGame $login, password, $gameId")
    for {
      cookies <- WebFetcher.loginPost(login, password)
      doc <- GameActions.joinGame(cookies, gameId)
    } yield doc
  }

  def makeMove(login: String, password: String, gameId: String, fullMove: FullMove)
              (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Future[String] = {
    logger.info("makeMove $login, password: $gameId, $fullMove")
    for {
      cookies <- WebFetcher.loginPost(login, password)
      result <- GameActions.makeMove(cookies, gameId, fullMove)
    } yield result
  }

  def getActiveGames(login: String, password: String)
                    (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Future[immutable.Seq[GameMetadata]] = {
    logger.info("getActiveGames $login, password")
    for {
      cookies <- WebFetcher.loginPost(login, password)
      doc <- WebFetcher.getGamesInProgressDoc(cookies)
    } yield ActiveGameListExtractor(doc)
  }

  def getGame(login: String, password: String, gameId: String)
             (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Future[GameBoard] = {
    logger.info("getGame $login, password, $gameId")
    for {
      cookies <- WebFetcher.loginPost(login, password)
      doc <- WebFetcher.getGameDetailsDoc(cookies, gameId)
    } yield GameDetailsExtractor(doc)
  }
}
