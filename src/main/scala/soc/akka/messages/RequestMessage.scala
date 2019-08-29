package soc.akka.messages

import akka.actor.typed.ActorRef
import soc.game.inventory.Inventory
import soc.game.player.PlayerStateManager
import soc.game.GameState

sealed trait RequestMessage[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER]] extends GameMessage {
  val gameId: Int
  val playerState: GameState[PLAYER]
  val inventory: GAME
  val playerId: Int
  val respondTo: ActorRef[Response]
}

object RequestMessage {
  case class InitialPlacementRequest[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER]](gameId: Int, playerState: GameState[PLAYER], inventory: GAME, playerId: Int, first: Boolean, respondTo: ActorRef[Response]) extends RequestMessage[GAME, PLAYER]
  case class DiscardCardRequest[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER]](gameId: Int, playerState: GameState[PLAYER], inventory: GAME, playerId: Int, currentTurn: Int, respondTo: ActorRef[Response]) extends RequestMessage[GAME, PLAYER]
  case class MoveRobberRequest[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER]](gameId: Int, playerState: GameState[PLAYER], inventory: GAME, playerId: Int, respondTo: ActorRef[Response]) extends RequestMessage[GAME, PLAYER]
  case class MoveRequest[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER]](gameId: Int, playerState: GameState[PLAYER], inventory: GAME, playerId: Int, respondTo: ActorRef[Response]) extends RequestMessage[GAME, PLAYER]

  //case class ControllerRequestMessage(playerState: GameState, playerId: Int, first: Boolean, respondTo: ActorRef[Response], move: CatanMove) extends RequestMessage
}

