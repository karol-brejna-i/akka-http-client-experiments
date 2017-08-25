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
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging
import org.fbc.experiments.akkahttpfetch.actuators.{GameActions, WebFetcher}
import org.fbc.experiments.akkahttpfetch.api.GameApi
import org.fbc.experiments.akkahttpfetch.model._
import org.fbc.experiments.akkahttpfetch.utils.DebugUtils

import scala.concurrent._
import scala.util.{Failure, Success}

object Main extends App with GameApi with StrictLogging with DebugUtils {
  implicit val system: ActorSystem = ActorSystem("fbc")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val password = System.getenv().get("BAJ_PASSWORD")
  val login = System.getenv().get("BAJ_LOGIN")

  private def testGetActiveGameList() = {
    val responseF = getActiveGames(login, password)
    responseF onComplete {
      case Success(response) => logResult(response)
      case Failure(e) => logger.error("Error from the future", e)
    }
  }

  private def testGetGameDetails(gameId: String) = {
    val responseF = getGame(login, password, gameId)
    responseF onComplete {
      case Success(response) => logResult(response)
      case Failure(e) => logger.error("Error from the future", e)
    }
  }

  private def testCreateGame() = {
    val responseF = createGame(login, password, GameInvitation("test - DO NOT JOIN", None, None, Some("testuser"), SYMMETRICAL))
    responseF onComplete {
      case Success(result) => logResult(result)
      case Failure(e) => {
        logger.error("Error from the future", e)
        logger.error("stack: {}", e.getMessage)
      }
    }
  }

  private def testJoinGame() = {
    val inviteId = "137836"
    val responseF = joinGame(login, password, inviteId)
    responseF onComplete {
      case Success(result) => logResult(result)
      case Failure(e) => {
        logger.error("Error from the future", e)
        logger.error("stack: {}", e.getMessage)
      }
    }
  }

  def testMakeMove(): Unit = {
    val gameId = "37683"
    val r = for {
      cookies <- WebFetcher.loginPost(login, password)
      result <- GameActions.makeMove(cookies, gameId,
        FullMove(
          Move(CAPTURE, Some("A5"), Some("A4")),
          Move(STACK, Some("A4"), Some("A3"))
        )
      )
    } yield result

    r.foreach(logResult(_))
  }

  logger.info("Before fetch")
//  testGetActiveGameList()
  testGetGameDetails("37709")
  logger.info("This is it....")
  harakiri(4)
}
