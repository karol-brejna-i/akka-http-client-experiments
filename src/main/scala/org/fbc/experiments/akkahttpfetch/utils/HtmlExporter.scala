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

import akka.http.scaladsl.settings.ConnectionPoolSettings
import com.typesafe.scalalogging.StrictLogging
import org.fbc.experiments.akkahttpfetch.model.{GameBoard, Piece}

import scala.collection.immutable

trait HtmlExporter extends StrictLogging with DebugUtils {

  private val fieldNames: immutable.Seq[String] = List(
    "A5", "A4", "A3", "A2", "A1",
    "B6", "B5", "B4", "B3", "B2", "B1",
    "C7", "C6", "C5", "C4", "C3", "C2", "C1",
    "D8", "D7", "D6", "D5", "D4", "D3", "D2", "D1",
    "E8", "E7", "E6", "E5", "-", "E4", "E3", "E2", "E1",
    "F8", "F7", "F6", "F5", "F4", "F3", "F2", "F1",
    "G7", "G6", "G5", "G4", "G3", "G2", "G1",
    "H6", "H5", "H4", "H3", "H2", "H1",
    "I5", "I4", "I3", "I2", "I1")

  private val positionLookup = fieldNames.map { case "-" => "E-"; case x => x }.groupBy(_.substring(0, 1)).toList

  private val srtdList = positionLookup.sortWith(_._1 < _._1).map(_._2)

  val columnHead =
    """|        <td style="vertical-align:middle;">
       |            <table border="0" cellpadding="0" cellspacing="0">
       |                <tbody>
    """.stripMargin

  val columnTail =
    """|                </tbody>
       |            </table>
       |            </td>
    """.stripMargin

  val tableHead =
    """|<table border="0" cellpadding="0" cellspacing="0" style="background-image:url(img/miniplateau.gif);">
       |    <tbody>
       |    <tr>
       |        <td><img src="img/_.gif" width="10" height="200"></td>
    """.stripMargin

  val tableTail =
    """|    </tr>
       |    </tbody>
       |</table>
    """.stripMargin


  private def encodePiece(piece: Piece): String = {
    val value = piece.value match {
      case "TZAAR" => "1"
      case "TZAARA" => "2"
      case "TOOT" => "3"
    }
    val color = if (piece.colour == "BLACK") "2" else "1"
    value + color
  }

  private def getPieceImg(piece: Option[Piece]): String = {
    piece match {
      case None => "img/_.gif"
      case Some(p) => s"""img/${encodePiece(p)}m.gif"""
    }
  }

  private def getStackRepresentation(piece: Option[Piece]): String = {

    piece match {
      case None => "&nbsp;"
      case Some(p) => if (p.stack == 1)  "&nbsp;" else s"""<span style="color:red">${p.stack}</span>"""
    }

  }

  def emitField(fieldName: String)(implicit pieces: Map[String, Piece]): String = {
    s"""|                <tr>
        |                    <td class="clMiniH" style="background-image:url(${getPieceImg(pieces.get(fieldName))})">
        |                        <div style="height:20;width:20">${getStackRepresentation(pieces.get(fieldName))}</div>
        |                    </td>
        |                </tr>
  """.stripMargin
  }

  def emitColumn(strings: Seq[String])(implicit pieces: Map[String, Piece]): String = {
    columnHead + strings.map(emitField(_)).mkString("\n") + columnTail
  }

  def export(game: GameBoard): String = {
    implicit val pieces: Map[String, Piece] = game.piecePositions
    val columns = srtdList.map(column => {
      emitColumn(column)
    }).mkString("\n")

    val result = tableHead + columns + tableTail
    val header =
      """
        |.clMiniH {
        |  color: blue;
        |  font-family: Tahoma, Verdana, Arial;
        |  font-size: 8pt;
        |  font-weight: bold;
        |  text-align:right;
        |  vertical-align:middle;
        |}
      """.stripMargin
    s"""<html><header><style>$header</style></header><body>$result</body>"""
  }
}
