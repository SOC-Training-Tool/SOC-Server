package soc.akka.messages

import akka.actor.typed.ActorRef
import soc.akka.GameStateHolder
import soc.akka.MoveResultProviderMessage.MoveResultProviderMessage
import soc.game.board.{BoardConfiguration, CatanBoard}
import soc.game.inventory.Inventory
import soc.game.player.PlayerStateHelper
import soc.game.{CatanMove, GameState}
import soc.storage.{GameId, MoveEntry}

trait CatanMessage

trait GameMessage extends CatanMessage
trait PlayerMessage extends CatanMessage

case class PlayerDoMove(playerId: Int, move: CatanMove, respondTo: ActorRef[MoveResponse]) extends GameMessage
case object PlayerStartGame extends GameMessage
case class StateMessage[GAME <: Inventory[GAME],PLAYERS <: Inventory[PLAYERS]](states: GameStateHolder[GAME, PLAYERS], message: CatanMessage)
case class PlayerAdded(playerId: Int) extends CatanMessage
case object EndGame extends CatanMessage

case class MoveEntryMessage(move: MoveEntry) extends GameMessage
case class SaveGameMessage[BOARD <: BoardConfiguration](gameId: GameId, initBoard: BOARD, players: Map[(String, Int), Int]) extends GameMessage

case object Terminate extends GameMessage


case class ErrorMessage(ex: Any) extends CatanMessage
