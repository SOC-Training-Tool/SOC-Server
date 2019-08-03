package soc.akka.messages

import soc.akka.GameStateHolder
import soc.game.GameState
import soc.sql.MoveEntry

trait CatanMessage

trait GameMessage extends CatanMessage
trait PlayerMessage extends CatanMessage

case class StateMessage(states: GameStateHolder, message: CatanMessage)

case class MoveEntryMessage(move: MoveEntry) extends GameMessage
case object Terminate extends GameMessage


case class ErrorMessage(ex: Any) extends CatanMessage
