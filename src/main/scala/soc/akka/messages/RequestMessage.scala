package soc.game.messages

import akka.actor.typed.ActorRef
import soc.game.GameState

sealed trait RequestMessage extends GameMessage {
  val playerState: GameState
  val playerId: Int
  val respondTo: ActorRef[Response]
}

object RequestMessage {
  case class InitialPlacementRequest(playerState: GameState, playerId: Int, first: Boolean, respondTo: ActorRef[Response]) extends RequestMessage
  case class DiscardCardRequest(playerState: GameState, playerId: Int, respondTo: ActorRef[Response]) extends RequestMessage
  case class MoveRobberRequest(playerState: GameState, playerId: Int, respondTo: ActorRef[Response]) extends RequestMessage
  case class MoveRequest(playerState: GameState, playerId: Int, respondTo: ActorRef[Response]) extends RequestMessage
}

