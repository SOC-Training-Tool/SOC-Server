package soc.akka.messages

import akka.actor.typed.ActorRef
import soc.game.inventory.Inventory
import soc.game.player.PlayerStateManager
import soc.game.{CatanMove, GameState}

sealed trait RequestMessage[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER]] extends GameMessage {
  val gameId: Int
  val playerState: GameState[PLAYER]
  val inventory: GAME
  val playerId: Int
  val respondTo: ActorRef[MoveResponse]
}

object RequestMessage {
  case class InitialPlacementRequest[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER]](gameId: Int, playerState: GameState[PLAYER], inventory: GAME, playerId: Int, first: Boolean, respondTo: ActorRef[MoveResponse]) extends RequestMessage[GAME, PLAYER]
  case class DiscardCardRequest[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER]](gameId: Int, playerState: GameState[PLAYER], inventory: GAME, playerId: Int, respondTo: ActorRef[MoveResponse]) extends RequestMessage[GAME, PLAYER]
  case class MoveRobberRequest[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER]](gameId: Int, playerState: GameState[PLAYER], inventory: GAME, playerId: Int, respondTo: ActorRef[MoveResponse]) extends RequestMessage[GAME, PLAYER]
  case class MoveRequest[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER]](gameId: Int, playerState: GameState[PLAYER], inventory: GAME, playerId: Int, respondTo: ActorRef[MoveResponse]) extends RequestMessage[GAME, PLAYER]

  //case class ControllerRequestMessage(playerState: GameState, playerId: Int, first: Boolean, respondTo: ActorRef[Response], move: CatanMove) extends RequestMessage
}

