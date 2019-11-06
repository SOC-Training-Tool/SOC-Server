package soc.behaviors.messages

import akka.actor.typed.ActorRef
import soc.board.BoardConfiguration
import soc.inventory.Inventory
import soc.playerRepository.ReceiveMoveFromClient
import soc.state.GameState
import soc.storage.{GameId, MoveEntry}

trait CatanMessage

trait GameMessage extends CatanMessage
trait PlayerMessage extends CatanMessage

case class PlayerDoMove(playerId: Int, move: ReceiveMoveFromClient, respondTo: ActorRef[MoveResponse]) extends GameMessage
case object PlayerStartGame extends GameMessage
case class StateMessage[GAME <: Inventory[GAME]](states: GameState[GAME], message: CatanMessage)
case class PlayerAdded(playerId: Int) extends CatanMessage
case object EndGame extends CatanMessage

case class MoveEntryMessage(move: MoveEntry) extends GameMessage
case class SaveGameMessage[BOARD <: BoardConfiguration](gameId: GameId, initBoard: BOARD, players: Map[(String, Int), Int]) extends GameMessage

case object Terminate extends GameMessage


case class ErrorMessage(ex: Any) extends CatanMessage
