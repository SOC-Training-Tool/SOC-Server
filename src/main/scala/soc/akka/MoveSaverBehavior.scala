package soc.akka

import akka.actor.typed.scaladsl.Behaviors
import soc.akka.messages.{GameMessage, MoveEntryMessage, Terminate}
import soc.sql.MoveEntry
import soc.storage.MoveSaver

case class DoMove()

object MoveSaverBehavior {

  def moveSaverBehavior(saver: MoveSaver) = Behaviors.receiveMessage[GameMessage] {

    case MoveEntryMessage(entry) =>
      saver.saveMove(entry)
      Behaviors.same

    case Terminate =>
      Behaviors.stopped
  }

}
