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

package org.fbc.experiments.akkahttpfetch.extractors

import com.typesafe.scalalogging.StrictLogging
import org.fbc.experiments.akkahttpfetch.DocCleaner
import org.fbc.experiments.akkahttpfetch.model.GameMetadata

import scala.collection.immutable
import scala.xml.{Elem, NodeSeq}

object ActiveGameListExtractor extends StrictLogging with DocCleaner {
  def apply(doc: String) = extractData(doc)

  def extractData(doc: String): immutable.Seq[GameMetadata] = {
    logger.info("getting games in progress")

    val docXml: Elem = getXML(doc)
    val r = (((docXml \ "body" \ "div" \ "div")(3) \ "div")(1) \ "div" \ "div")(3)

    val games = getGamesInProgress(r)
    games.map( extractGameListItem(_) )
  }

  private def getGamesInProgress(gameListNode: NodeSeq) = {
    logger.info("getGamesInProgress")
//    val idx = gameListPage.toHtml.indexOf("You have to log in to access this page!")
//    if (idx > 0) {
//      logger.warn(s"Obtaining games in progress, but not logged in.")
//    }
//    val items = gameListPage >?> elementList("#dvEnCours> div[class^=clLigne]")
    (gameListNode \ "div").filter( it => (it \ "@class").text.matches("clLigne.") )
  }

  private def extractGameListItem(node: NodeSeq) = {
    logger.info(s"extractGameListItem")
    val elements = node \ "div"
    val gameId = elements(0).text.trim
    val gameName = elements(1).text.trim
    val users = elements(2) \ "a" \ "span"
    val userOnMove = users.filter( it => (it \ "@style").text.contains("bold") ).map(_.text).head
    val userWhite = users(0).text
    val userBlack = users(1).text
    val sideOnMove = if (userOnMove == userWhite) "WHITE" else "BLACK"

    new GameMetadata(gameId, gameName, userWhite, userBlack, sideOnMove)
  }
}
