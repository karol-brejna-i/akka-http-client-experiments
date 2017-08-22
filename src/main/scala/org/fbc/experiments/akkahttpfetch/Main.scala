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

package org.fbc.experiments.akkahttpfetch

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.HttpCookie
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging
import org.fbc.experiments.akkahttpfetch.actuators.{GameActions, WebFetcher}
import org.fbc.experiments.akkahttpfetch.extractors.{ActiveGameListExtractor, GameDetailsExtractor}
import org.fbc.experiments.akkahttpfetch.model._
import org.fbc.experiments.akkahttpfetch.utils.DebugUtils

import scala.concurrent._
import scala.util.{Failure, Success}


object Main extends App with StrictLogging with DebugUtils {
  implicit val system: ActorSystem = ActorSystem("fbc")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val password = System.getenv().get("BAJ_PASSWORD")
  val login = System.getenv().get("BAJ_LOGIN")

  private def getGamesInProgressDoc() = {
    val responseF = WebFetcher.getGamesInProgressDoc(login, password)
    responseF onComplete {
      case Success(response) => logger.info("result! {}", response)
      case Failure(e) => logger.error("Error from the future", e)
    }
  }

  private def getActiveGameList() = {
    val responseF = WebFetcher.getGamesInProgressDoc(login, password)
    responseF onComplete {
      case Success(response) => {
        val result = ActiveGameListExtractor.extractData(response)
        logger.info("result! {}", result.size)
        logger.info("result! {}", result)
      }
      case Failure(e) => logger.error("Error from the future", e)
    }

    HttpCookie
  }

  private def startNewGame() = {
    val responseF = GameActions.startNewGame(login, password, GameInvitation("test1", None, None, Some("testuser"), SYMMETRICAL))
    responseF onComplete {
      case Success(result) => logResult(result)
      case Failure(e) => {
        logger.error("Error from the future", e)
        logger.error("stack: {}", e.getMessage)
      }
    }
  }

  private def joinGame() = {
    val inviteId = "37807"
    val responseF = GameActions.joinGame(login, password, inviteId)
    responseF onComplete {
      case Success(result) => logResult(result)
      case Failure(e) => {
        logger.error("Error from the future", e)
        logger.error("stack: {}", e.getMessage)
      }
    }
  }

  def makeMove(): Unit = {
    val gameId = "37709"
    val result = for {
      cookies <- WebFetcher.loginPost(login, password)
      result <- GameActions.makeMove(cookies, gameId,
        FullMove(
          Move(CAPTURE, Some("C3"), Some("C6")),
          Move(STACK, Some("C6"), Some("E7"))
        )
      )
    } yield result
      logResult(result)
  }

  logger.info("Before fetch")
  makeMove()
  logger.info("This is it....")
  harakiri2(14)
}
