package soc.storage

import io.circe.Encoder
import io.circe.generic.semiauto._

import soc.game.MoveResult

case class MoveEntry (
  gameId: Int,
  moveNumber: Int,
  playerId: Int,
  playerOrderNumber: Int,
  move: MoveResult,
)

