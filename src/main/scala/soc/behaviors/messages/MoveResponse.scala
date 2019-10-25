package soc.behaviors.messages

import akka.actor.typed.ActorRef
import soc.moves.{CatanMove, MoveResult}
import protos.soc.game.{MoveResponse => ProtoMoveResponse}

case class MoveResponse(playerId: Int, move: CatanMove, respondTo: ActorRef[ProtoMoveResponse]) extends PlayerMessage
case class ResultResponse(playerId: Int, result: MoveResult) extends PlayerMessage