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

package org.fbc.experiments.akkahttpfetch.utils

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.{ClientTransport, Http}
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.Future

trait ProxyTools extends StrictLogging {

  def getProxySettingsFromEnv()(implicit system: ActorSystem): ConnectionPoolSettings = {
    val proxy = System.getenv().getOrDefault("http_proxy", "")
    getProxySettings(proxy)
  }

  def getProxySettings(envProxyValue: String)(implicit system: ActorSystem): ConnectionPoolSettings = {
    if (!envProxyValue.isEmpty) {
      val parts = envProxyValue.split(":")
      if (parts.size != 3) {
        logger.error(s"Invalid proxy env value: $envProxyValue. Cannot set up proxy.")
      } else {
        val proxyPort = parts(2)
        val proxyHost = parts(1).substring(2)
        logger.debug("Using proxy settings host: {}, port: {}.", proxyHost, proxyPort)

        val transport = ClientTransport.httpsProxy(InetSocketAddress.createUnresolved(proxyHost, proxyPort.toInt))
        return ConnectionPoolSettings(system).withTransport(transport)
      }
    }
    ConnectionPoolSettings(system)
  }

  def singleRequestWithProxy(request: HttpRequest)
                            (implicit system: ActorSystem, fm: akka.stream.Materializer)
  : Future[HttpResponse] = {
    Http().singleRequest(request, settings = getProxySettingsFromEnv())
  }
}
