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
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging
import org.fbc.experiments.akkahttpfetch.utils.ProxyTools

import scala.collection.immutable.{Map, Seq}
import scala.concurrent.{ExecutionContext, Future}

class WebFetcher

object WebFetcher extends StrictLogging with ProxyTools {
  private val loginUri = "http://www.boiteajeux.net/gestion.php"
  private val inProgressUri = "http://www.boiteajeux.net/index.php?p=encours"
  private val gameDetailsUri = "http://www.boiteajeux.net/jeux/tza/partie.php?id=%s"

  def loginPost(login: String, password: String)
               (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Future[Seq[HttpCookiePair]] = {
    logger.info("login")
    val form = Map("p" -> "", "pAction" -> "login", "username" -> login, "password" -> password)

    val entity = akka.http.scaladsl.model.FormData(form).toEntity(HttpCharsets.`UTF-8`)
    val loginRequest = HttpRequest(uri = loginUri, method = HttpMethods.POST, entity = entity)
    val responseFuture: Future[HttpResponse] = singleRequestWithProxy(loginRequest)

    fixCookies(responseFuture.map(_.headers.collect { case `Set-Cookie`(x) => HttpCookiePair.apply(x.name, x.value) }))
  }

  def getGameDetailsDoc(cookies: Seq[HttpCookiePair], gameId: String)
                       (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer)
  : Future[String] = {
    logger.info("getGameDetailsDoc")
    for {
      response <- WebFetcher.getGamesDetailsResponse(cookies, gameId)
      doc <- Unmarshal(response.entity).to[String]
    } yield doc
  }

  def getGamesInProgressDoc(login: String, password: String)
                           (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer)
  : Future[String] = {
    logger.info("tryfetch")
    for {
      cookies <- WebFetcher.loginPost(login, password)
      response <- WebFetcher.getGamesInProgressResponse(cookies)
      doc <- Unmarshal(response.entity).to[String]
    } yield doc
  }

  private def getGamesInProgressResponse(cookies: Seq[HttpCookiePair])
                                        (implicit system: ActorSystem, materializer: ActorMaterializer)
  : Future[HttpResponse] = {
    logger.info("getGamesInProgress")
    val headers: Seq[HttpHeader] = if (cookies.isEmpty) Seq.empty else Seq(Cookie(cookies))
    val req = HttpRequest(uri = inProgressUri, method = HttpMethods.GET, headers = headers)
    singleRequestWithProxy(req)
  }

  private def getGamesDetailsResponse(cookies: Seq[HttpCookiePair], gameId: String)
                                     (implicit system: ActorSystem, materializer: ActorMaterializer)
  : Future[HttpResponse] = {
    logger.info("getGamesInProgress")
    val headers: Seq[HttpHeader] = if (cookies.isEmpty) Seq.empty else Seq(Cookie(cookies))
    val req = HttpRequest(uri = String.format(gameDetailsUri, gameId), method = HttpMethods.GET, headers = headers)
    singleRequestWithProxy(req)
  }

  def fixCookies(cookies: Future[Seq[HttpCookiePair]])(implicit ec: ExecutionContext): Future[Seq[HttpCookiePair]] = {
    import scala.collection.breakOut
    cookies.map(c => c.groupBy(_.name).map(_._2.last)(breakOut))
  }

}
