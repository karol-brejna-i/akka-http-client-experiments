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

sealed trait InitialBoardSetup
case object SYMMETRICAL extends InitialBoardSetup
case object RANDOM extends InitialBoardSetup


sealed trait MoveType
case object CAPTURE extends MoveType
case object STACK extends MoveType
case object PASS extends MoveType

sealed trait GameRepresentationTrait

case class GameMetadata(gameId: String, gameName: String, userWhite: String, userBlack: String, sideOnMove: String) extends GameRepresentationTrait
case class Piece(colour: String, value: String, stack: Integer) extends GameRepresentationTrait
case class GameBoard(piecePositions: Map[String, Piece], metadata: GameMetadata) extends GameRepresentationTrait
case class Move(moveType: MoveType, from: Option[String], to: Option[String]) extends GameRepresentationTrait
case class FullMove(firstMove: Move, secondMove: Move) extends GameRepresentationTrait

case class GameInvitation(gameName: String, eloFrom: Option[String], eloTo: Option[String],
                          invitedPlayer: Option[String], setup: InitialBoardSetup = SYMMETRICAL)

object GameBoard {
  /**
    * Fields names (one by one) in emulation of the hexagonal TZAAR board as s quadratic 9x9 array.
    * See: http://iuuk.mff.cuni.cz/~vesely/tzaar/thesis.pdf
    *
    * I am not sure if it sticks well to the board notation shown in TZAAR manual (and on BAJ page).
    *
    * @return
    */
  def fieldNames: immutable.Seq[String] = List(
    "A1", "B1", "C1", "D1", "E1", "-", "-", "-", "-",
    "A2", "B2", "C2", "D2", "E2", "F1", "-", "-", "-",
    "A3", "B3", "C3", "D3", "E3", "F2", "G1", "-", "-",
    "A4", "B4", "C4", "D4", "E4", "F3", "G2", "H1", "-",
    "A5", "B5", "C5", "D5", "-", "F4", "G3", "H2", "I1",
    "-", "B6", "C6", "D6", "E5", "F5", "G4", "H3", "I2",
    "-", "-", "C7", "D7", "E6", "F6", "G5", "H4", "I3",
    "-", "-", "-", "D8", "E7", "F7", "G6", "H5", "I4",
    "-", "-", "-", "-", "E8", "F8", "G7", "H6", "I5")
}
