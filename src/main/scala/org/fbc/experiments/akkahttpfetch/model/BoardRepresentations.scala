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

package org.fbc.experiments.akkahttpfetch.model
import scala.collection.immutable

sealed trait BoardRepresentations {

  def fieldNames: Seq[String]

  /**
    * Converts logical board position ("A1", "E8", ...) to physical coordinate system used by given implementation
    * (i.e. BAJ)
    * @param position board position according to adopted Tzaar model (A1, A2, ....)
    */
  def toPhysicalCoordinates(position: String): Any

  def validatePosition(position: String): Unit = {
    require(position.length == 2, "Position description has exactly 2 characters")
    require(('A' to 'I').contains(position.head), "Position description's format is XY (X='A'..'I', Y='1'..'8')")
    require(('1' to '8').contains(position.last), "Position description's format is XY (X='A'..'I', Y='1'..'8')")
    require(fieldNames.contains(position), s"The position ${position} is outside TZAAR board")
  }
}

class BajBoard extends BoardRepresentations {
  override def fieldNames: Seq[String] = List(
    "A5", "A4", "A3", "A2", "A1",
    "B6", "B5", "B4", "B3", "B2", "B1",
    "C7", "C6", "C5", "C4", "C3", "C2", "C1",
    "D8", "D7", "D6", "D5", "D4", "D3", "D2", "D1",
    "E8", "E7", "E6", "E5", "-", "E4", "E3", "E2", "E1",
    "F8", "F7", "F6", "F5", "F4", "F3", "F2", "F1",
    "G7", "G6", "G5", "G4", "G3", "G2", "G1",
    "H6", "H5", "H4", "H3", "H2", "H1",
    "I5", "I4", "I3", "I2", "I1")

  private val positionLookup = fieldNames.map{case "-" => "E-"; case x => x}.groupBy(_.substring(0,1))

  override def toPhysicalCoordinates(position: String): (Int, Int) = {
    validatePosition(position)
    val column = position.head - 65
    val row = positionLookup(position.substring(0,1)).indexOf(position) + (if (column > 4) column - 4 else 0)
    (row, column)
  }
}

