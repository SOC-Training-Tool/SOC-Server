package soc.akka.messages

import akka.actor.typed.ActorRef
import soc.akka.GameStateHolder
import soc.akka.MoveResultProviderMessage.MoveResultProviderMessage
import soc.game.board.BoardConfiguration
import soc.game.inventory.Inventory
import soc.game.player.PlayerStateManager
import soc.game.{CatanMove, GameState}
import soc.storage.MoveEntry

trait CatanMessage

trait GameMessage extends CatanMessage
trait PlayerMessage extends CatanMessage

case object ReceivedDiscard extends CatanMessage

case class StateMessage[GAME <: Inventory[GAME],PLAYERS <: Inventory[PLAYERS]](states: GameStateHolder[GAME, PLAYERS], message: CatanMessage)

case class MoveEntryMessage(move: MoveEntry) extends GameMessage
case class SaveGameMessage[BOARD <: BoardConfiguration](gameId: Int, initBoard: BOARD, players: Map[(String, Int), Int]) extends GameMessage

case object Terminate extends GameMessage


case class ErrorMessage(ex: Any) extends CatanMessage
