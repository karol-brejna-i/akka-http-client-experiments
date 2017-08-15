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

import akka.http.scaladsl.model.headers.HttpCookiePair
import org.scalatest.Matchers._
import org.scalatest.{BeforeAndAfterEach, FlatSpec}

import scala.collection.{breakOut, immutable}
import scala.concurrent.Future
import scala.util.{Failure, Success}

class WebFetcherTest extends FlatSpec with BeforeAndAfterEach {

  val cookiesString = "style_cookie=null; phpbb3_fi8gb_u=1; phpbb3_fi8gb_k=; phpbb3_fi8gb_sid=e9a613a4219ab794b156420cfb7cb3ad; __utmt=1; __utma=156074536.1851143840.1501391571.1502719518.1502725655.31; __utmb=156074536.3.10.1502725655; __utmc=156074536; __utmz=156074536.1501391571.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none)"
  val duplicatedCookiesString = "phpbb3_fi8gb_u=1; phpbb3_fi8gb_k=; phpbb3_fi8gb_sid=bbb5aee1aee4bcdb42d087e8f506217a; PHPSESSID=aao96jnolv3od2hgmk392lk9a5; phpbb3_fi8gb_u=55655; phpbb3_fi8gb_k=; phpbb3_fi8gb_sid=e2c55e261c5ad8f806fd0601eb9f224b"

  private def createHttpCookiePairs(s: String): immutable.Seq[HttpCookiePair] = {
    val pairs = s.split(";")
    pairs.map(p => {
      val pair = p.trim.split("=", -1)
      HttpCookiePair.apply(pair(0), pair(1))
    }
    ).to[collection.immutable.Seq]
  }

  val duplicatedCookies = createHttpCookiePairs(duplicatedCookiesString)


  behavior of "WebFetcherTest"


  // There is a problem with handling the cookies here.
  // When login, BAJ forces the following sequence (note set-cookie  phpbb3_fi8gb_u):
  // ```
  //< HTTP/1.1 302 Moved Temporarily
  //< Date: Tue, 15 Aug 2017 04:23:48 GMT
  //< Server: Apache
  //< X-Powered-By: PHP/5.6.22
  //< Expires: Thu, 19 Nov 1981 08:52:00 GMT
  //< Last-Modified: Tue, 15 Aug 2017 04:23:48 GMT
  //< Cache-Control: no-store, no-cache, must-revalidate, post-check=0, pre-check=0
  //< x-ua-compatible: ie=edge
  //* Added cookie phpbb3_fi8gb_u="1" for domain boiteajeux.net, path /, expire 1534307028
  //< Set-Cookie: phpbb3_fi8gb_u=1; expires=Wed, 15-Aug-2018 04:23:48 GMT; path=/; domain=.boiteajeux.net; HttpOnly
  //* Added cookie phpbb3_fi8gb_k="" for domain boiteajeux.net, path /, expire 1534307028
  //< Set-Cookie: phpbb3_fi8gb_k=; expires=Wed, 15-Aug-2018 04:23:48 GMT; path=/; domain=.boiteajeux.net; HttpOnly
  //* Added cookie phpbb3_fi8gb_sid="18007922742a7a2125e89de56dd2c7e6" for domain boiteajeux.net, path /, expire 1534307028
  //< Set-Cookie: phpbb3_fi8gb_sid=18007922742a7a2125e89de56dd2c7e6; expires=Wed, 15-Aug-2018 04:23:48 GMT; path=/; domain=.boiteajeux.net; HttpOnly
  //* Added cookie PHPSESSID="li8u7ckko8lhafdoaitho1mre0" for domain www.boiteajeux.net, path /, expire 0
  //< Set-Cookie: PHPSESSID=li8u7ckko8lhafdoaitho1mre0; path=/; secure; HttpOnly
  //* Replaced cookie phpbb3_fi8gb_u="55655" for domain boiteajeux.net, path /, expire 1534307028
  //< Set-Cookie: phpbb3_fi8gb_u=55655; expires=Wed, 15-Aug-2018 04:23:48 GMT; path=/; domain=.boiteajeux.net; HttpOnly
  //* Replaced cookie phpbb3_fi8gb_k="" for domain boiteajeux.net, path /, expire 1534307028
  //< Set-Cookie: phpbb3_fi8gb_k=; expires=Wed, 15-Aug-2018 04:23:48 GMT; path=/; domain=.boiteajeux.net; HttpOnly
  //* Replaced cookie phpbb3_fi8gb_sid="92e5dbf6f3dd39c27c811cb6da21fe94" for domain boiteajeux.net, path /, expire 1534307028
  //< Set-Cookie: phpbb3_fi8gb_sid=92e5dbf6f3dd39c27c811cb6da21fe94; expires=Wed, 15-Aug-2018 04:23:48 GMT; path=/; domain=.boiteajeux.net; HttpOnly
  //< Pragma: no-cache
  // ```
  // Some cookies get replaced here, while the method I use:
  // `esponseFuture.map(_.headers.collect { case `Set-Cookie`(x) => HttpCookiePair.apply(x.name, x.value) })`
  // produces doubles.
  // Need to simulate replace mechanizm.
  it should "simulate cookie replace when there are two cookies by the same name" in {
    import scala.concurrent.ExecutionContext.Implicits.global
    val cookiesF = Future{duplicatedCookies}
    val fixedF = WebFetcher.fixCookies(cookiesF)

    fixedF onComplete {
      case Success(fixed) =>
        val fixedAsMap : Map[String, String] = fixed.map(i => (i.name -> i.value))(breakOut)

        fixedAsMap should (contain key ("phpbb3_fi8gb_u") and not contain value ("1"))
        fixedAsMap should (contain key ("phpbb3_fi8gb_u") and contain value ("55655"))
        fixedAsMap should (contain key ("phpbb3_fi8gb_sid") and not contain value ("bbb5aee1aee4bcdb42d087e8f506217a"))
        fixedAsMap should (contain key ("phpbb3_fi8gb_sid") and contain value ("e2c55e261c5ad8f806fd0601eb9f224b"))
      case Failure(e@_) =>
    }
  }
}
