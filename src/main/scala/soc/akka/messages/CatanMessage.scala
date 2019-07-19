package soc.game.messages

import soc.behaviors.GameStateHolder
import soc.game.GameState

trait CatanMessage

trait GameMessage extends CatanMessage
trait PlayerMessage extends CatanMessage

case class StateMessage(states: GameStateHolder, message: CatanMessage)

case object Terminate extends GameMessage


case class ErrorMessage(ex: Any) extends CatanMessage
