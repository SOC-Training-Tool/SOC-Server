package soc.akka.messages

import akka.actor.typed.ActorRef
import soc.akka.GameStateHolder
import soc.game.inventory.Inventory
import soc.game.player.PlayerStateManager
import soc.game.{CatanMove, GameState}
import soc.sql.MoveEntry

trait CatanMessage

trait GameMessage extends CatanMessage
trait PlayerMessage extends CatanMessage

case object ReceivedDiscard extends CatanMessage

case class StateMessage[GAME <: Inventory[GAME],PLAYERS <: Inventory[PLAYERS]](states: GameStateHolder[GAME, PLAYERS], message: CatanMessage)

case class MoveEntryMessage(move: MoveEntry) extends GameMessage
case class MoveResultProviderMessage[GAME <: Inventory[GAME]](gameState: GameState[GAME], id: Int, move: CatanMove.Move, respondTo: ActorRef[Response]) extends GameMessage
case object Terminate extends GameMessage


case class ErrorMessage(ex: Any) extends CatanMessage
