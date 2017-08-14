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
 *
 */


package org.fbc.experiments.akkahttpfetch

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging

import scala.collection.immutable._
//import scala.collection.parallel.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class WebFetcher

object WebFetcher extends StrictLogging {
  private val basicUri = "http://www.boiteajeux.net"
  private val loginUri = "http://www.boiteajeux.net/gestion.php"
  private val gameDetailsUri = "http://www.boiteajeux.net/jeux/tza/partie.php?id=%s"
  private val inProgressUri = "http://www.boiteajeux.net/index.php?p=encours"

  def loginPost(login: String, password: String)
               (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Future[Seq[HttpCookiePair]] = {
    logger.info("login")
    val form = Map("p" -> "", "pAction" -> "login", "username" -> login, "password" -> password)

    val entity = akka.http.scaladsl.model.FormData(form).toEntity(HttpCharsets.`UTF-8`)
    val loginRequest = HttpRequest(uri = loginUri, method = HttpMethods.POST, entity = entity)
    val responseFuture: Future[HttpResponse] = Http().singleRequest(loginRequest)

    responseFuture.map(_.headers.collect {
      case `Set-Cookie`(x) =>
        HttpCookiePair.apply(x.name, x.value)
//        (x.name, x.value)
//        Cookie(x.name, x.value)
    } )
  }


  def getGamesInProgressDoc(cookies: Future[Seq[HttpCookiePair]])
                           (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer)
                            : Future[HttpResponse] = {
    logger.info("getGamesInProgress")


    cookies.flatMap(c => {
      logger.info("my kukiz: {}", c )
      val headers: Seq[HttpHeader] = if (c.isEmpty) Seq.empty else Seq(Cookie(c))
      val req = HttpRequest(uri = inProgressUri, method = HttpMethods.GET, headers = headers)
      val resp = Http().singleRequest(req)

      resp
    })
  }

  def getGameDetailsDoc(cookies: Future[Seq[Cookie]], gameId: String): Unit = {
    logger.info("getGameDetailsDoc")
    //    browser.get(String.format(gameDetailsUri, gameId))
  }
}

object Main extends App with StrictLogging {
  implicit val system: ActorSystem = ActorSystem("fbc")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val password = System.getenv().get("BAJ_PASSWORD")
  val login = System.getenv().get("BAJ_LOGIN")

  def harakiri(agony: Int = 5): Unit = {
    val fs = (1 to agony).map { i =>
      Future {
        Thread.sleep(1000);
        i
      }
    }
    val f = Future.sequence(fs)
    logger.info("awaiting to die")
    Await.result(f, Duration.Inf)
    logger.info("done waiting")
    system.terminate()
  }

  private def tryfetch() = {
    val cookiesF = WebFetcher.loginPost(login, password)
    val doc = WebFetcher.getGamesInProgressDoc(cookiesF)


    val result = Await.result(doc, 10.seconds)

//    doc.onComplete {
//      case Success(response) => logger.info("result! {} {}", response.headers, response.entity)
//      case Failure(e) => logger.error("nie mam pana response i co mi pan zrobi !? {} ", e)
//    }
    result
  }

  logger.info("Before fetch")

  val response = tryfetch()

  logger.info("result! {} {}", response.headers, response.entity)
//  val cookiesF = WebFetcher.loginPost(login, password)
//  cookiesF onComplete {
//    case Success(c) => logger.info("cookies: {}", c)
//    case Failure(e) => logger.error("Error from the future", e)
//  }
  logger.info("This is it....")
  harakiri(10)
}