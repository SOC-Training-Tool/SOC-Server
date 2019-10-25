package soc.behaviors.messages

import akka.actor.typed.ActorRef
import soc.inventory.Inventory
import soc.inventory.Inventory.PublicInfo
import soc.state.GameState
import soc.storage.GameId

sealed trait RequestMessage[GAME <: Inventory[GAME]] extends GameMessage {
  val gameId: GameId
  val playerState: GameState[PublicInfo]
  val inventory: GAME
  val playerId: Int
  val respondTo: ActorRef[MoveResponse]
}

object RequestMessage {
  case class InitialPlacementRequest[GAME <: Inventory[GAME]](gameId: GameId, playerState: GameState[PublicInfo], inventory: GAME, playerId: Int, first: Boolean, respondTo: ActorRef[MoveResponse]) extends RequestMessage[GAME]
  case class DiscardCardRequest[GAME <: Inventory[GAME]](gameId: GameId, playerState: GameState[PublicInfo], inventory: GAME, playerId: Int, respondTo: ActorRef[MoveResponse]) extends RequestMessage[GAME]
  case class MoveRobberRequest[GAME <: Inventory[GAME]](gameId: GameId, playerState: GameState[PublicInfo], inventory: GAME, playerId: Int, respondTo: ActorRef[MoveResponse]) extends RequestMessage[GAME]
  case class MoveRequest[GAME <: Inventory[GAME]](gameId: GameId, playerState: GameState[PublicInfo], inventory: GAME, playerId: Int, respondTo: ActorRef[MoveResponse]) extends RequestMessage[GAME]

  //case class ControllerRequestMessage(playerState: GameState, playerId: Int, first: Boolean, respondTo: ActorRef[Response], move: CatanMove) extends RequestMessage
}

