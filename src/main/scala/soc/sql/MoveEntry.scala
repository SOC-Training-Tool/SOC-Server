package soc.sql

import soc.game.{CatanMove, MoveResult}

case class MoveEntry (
  gameId: Int,
  moveNumber: Int,
  playerId: Int,
  playerOrderNumber: Int,
  move: MoveResult[_],
)




