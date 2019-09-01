package soc.akka.messages

import soc.game.{CatanMove, MoveResult}

case class MoveResponse(playerId: Int, move: CatanMove) extends PlayerMessage
case class ResultResponse(playerId: Int, result: MoveResult) extends PlayerMessage