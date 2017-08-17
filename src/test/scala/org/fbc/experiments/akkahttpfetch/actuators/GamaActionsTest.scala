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

package org.fbc.experiments.akkahttpfetch.actuators

import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, FlatSpec}


class GamaActionsTest extends FlatSpec with BeforeAndAfterEach with ScalaFutures  {
  behavior of "GameActions"

  private def getDoc(filename: String): String = {
    import scala.io.Source
    Source.fromResource(filename).mkString
  }

  it should "extract gameId " in {
    import scala.concurrent.ExecutionContext.Implicits.global

    val doc = getDoc("createInviteResponse_success.html")
    val resultF = GameActions.interpreteNewGameResponse(doc)
    whenReady(resultF){ s =>
      s should be ("37745")
    }
  }

  it should "recognize user not logged in" in {
    import scala.concurrent.ExecutionContext.Implicits.global

    val doc = getDoc("createInviteResponse_not_logged.html")
    val resultF = GameActions.interpreteNewGameResponse(doc)
    whenReady(resultF.failed){ ex =>
      ex shouldBe an[IllegalStateException]
    }
  }
}
