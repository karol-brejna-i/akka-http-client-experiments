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

import org.fbc.experiments.akkahttpfetch.model.Piece

import scala.collection.immutable

def fieldNames: immutable.Seq[String] = List(
  "A5", "A4", "A3", "A2", "A1",
  "B6", "B5", "B4", "B3", "B2", "B1",
  "C7", "C6", "C5", "C4", "C3", "C2", "C1",
  "D8", "D7", "D6", "D5", "D4", "D3", "D2", "D1",
  "E8", "E7", "E6", "E5", "-", "E4", "E3", "E2", "E1",
  "F8", "F7", "F6", "F5", "F4", "F3", "F2", "F1",
  "G7", "G6", "G5", "G4", "G3", "G2", "G1",
  "H6", "H5", "H4", "H3", "H2", "H1",
  "I5", "I4", "I3", "I2", "I1")

val positionLookup = fieldNames.map{case "-" => "E-"; case x => x}.groupBy(_.substring(0,1)).toList

val srtdList = positionLookup.sortWith(_._1 < _._1).map(_._2)

val columnHead =
  """
    |        <td style="vertical-align:middle;">
    |            <table border="0" cellpadding="0" cellspacing="0">
    |                <tbody>
  """.stripMargin

val columnTail =
  """
    |                </tbody>
    |            </table>
|            </td>
  """.stripMargin

val tableTail =
  """
    |    </tr>
    |    </tbody>
    |</table>
  """.stripMargin

def emitField(fieldName: String, piece: Option[Piece] = None): String = {
  """
    |                <tr>
    |                    <td class="clMiniH" style="background-image:url(img/_.gif)">
    |                        <div style="height:20;width:20">&nbsp;</div>
    |                    </td>
    |                </tr>
    |
  """
  .stripMargin
}
def emitColumn(strings: Seq[String]): String = {
  columnHead + strings.map(emitField(_)).mkString("\n") + columnTail
}

val pole = srtdList.map(column => {
  emitColumn(column) + "\n"
}).mkString("\n")



//
//
//<tr>
//  <td class="clMiniH" style="background-image:url(img/12m.gif)">
//    <div style="height:20;width:20">&nbsp;</div>
//  </td>
//</tr>
//
//
