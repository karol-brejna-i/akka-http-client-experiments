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
import org.fbc.experiments.akkahttpfetch.model.{GameBoard, GameMetadata, Piece}
import org.fbc.experiments.akkahttpfetch.model.GameBoard.fieldNames

import scala.collection.immutable
import scala.xml.{Node, NodeSeq, XML}

object GameDetailsExtractor extends StrictLogging with DocCleaner {

  def apply(doc: String) = extractData(doc)

  def extractData(doc: String): GameBoard = {
    val docXml = getXML(doc)

    val boardElements = docXml  \ "body" \ "div" \ "table" \ "tbody" \ "tr" \ "td"

    val gameMetadataElement = (boardElements(0) \ "table" \ "tbody" \ "tr")(1) \ "td" \ "table" \ "tbody"
    val gameBoardElement = (boardElements(1) \ "div")(1)
    val gameHistoryElement = boardElements(2)

    val pieces = getPieces(gameBoardElement)
    GameBoard(mapPiecesToPositions(pieces), getGameMetadata(gameMetadataElement))
  }

//  def extractTurnMarker (node: NodeSeq) = {
//    node >> "input[name=pIdCoup]" >> attr("value")
//  }

  private def getGameMetadata(metadataElement : NodeSeq) = {
    val gameId = ((metadataElement  \ "tr")(0)).text.trim.split(" ")(1).substring(1)
    val gameName = ((metadataElement  \ "tr")(1)).text.trim.stripPrefix("\"").stripSuffix("\"").trim
    val lines = (metadataElement  \ "tr")(6) \ "td" \ "div" \ "table" \ "tbody" \ "tr"
    val isWhiteOnMove = (lines(0) \ "span" \@("style")).contains("red")
    GameMetadata(gameId, gameName, lines(0).text.trim, lines(2).text.trim, if (isWhiteOnMove) "WHITE" else "BLACK")
  }

  private def getPieces(boardElement: NodeSeq): Seq[Option[Piece]] = {
    val piecesElements = boardElement \ "div" \ "div"
    val pieceCodes = piecesElements.map(
      it => {
        val regexPattern = """url\('img/(\d)(\d).png'\)""".r
        for (m <- regexPattern.findFirstMatchIn(it.attributes.asAttrMap("style"))) yield (m.group(1), m.group(2))
      }
    )

    val pieces: Seq[Option[Piece]] = pieceCodes.map(
      _ match {
        case Some(c) => Some(decodePiece(c))
        case None => None
      }
    )

    val stacks: immutable.Seq[Int] = piecesElements.map(
      div => {
        val imgElements: Seq[Node] = div.child.filter(_.label == "img")

        imgElements match {
          case List() => 0
          case l:Seq[Node] => decodeStack(l.head.\@("src")).getOrElse(0)
        }
      }
    )

    (pieces, stacks).zipped.map(
      (a, height) => {
        a match {
          case Some(piece) => Some(Piece(piece.colour, piece.value, height))
          case None => None
        }
      }
    )
  }

  private def mapPiecesToPositions(pieces: Seq[Option[Piece]]) = {
    (pieces, fieldNameLookup).zipped.collect{case p if p._1.nonEmpty => (p._2 -> p._1.get)}.toMap
  }

  /**
    * Baj stores the fields (game details page) top-down from left-right.
    * After extracting board fields from BAJ page we'll get them in the following order A1, A2, A3, .... B1, B2, ...
    * Additionally:
    * BAJ doesn't hold "out of the board" fields, so we need to remove them ("-") from field names list,
    * but it keeps the empty center field (so we need to keep a name for it)
    * @return
    */
  def transposedFiledNames = fieldNames.sliding(9, 9).toList.reverse.transpose
  def fieldNameLookup = transposedFiledNames.flatten.zipWithIndex.filter{ it => (it._1 != "-" || it._2 == 40)}.unzip._1

  // the equivalent of above
  val bajFieldNames = List(
    "A5", "A4", "A3", "A2", "A1",
    "B6", "B5", "B4", "B3", "B2", "B1",
    "C7", "C6", "C5", "C4", "C3", "C2", "C1",
    "D8", "D7", "D6", "D5", "D4", "D3", "D2", "D1",
    "E8", "E7", "E6", "E5", "-", "E4", "E3", "E2", "E1",
    "F8", "F7", "F6", "F5", "F4", "F3", "F2", "F1",
    "G7", "G6", "G5", "G4", "G3", "G2", "G1",
    "H6", "H5", "H4", "H3", "H2", "H1",
    "I5", "I4", "I3", "I2", "I1")

  private def decodePiece(code: Tuple2[String, String]): Piece = {
    new Piece(code._1 match {
      case "1" => "TZAAR"
      case "2" => "TZAARA"
      case "3" => "TOOT"
    }, if (code._2 == "1") "WHITE" else "BLACK", 0)
  }

  private def decodeStack(code: String) = {
    logger.debug(s"decodeStack $code")
    val regexPattern = """img/num(\d).gif""".r
    for (m <- regexPattern.findFirstMatchIn(code)) yield m.group(1).toInt
  }
}
