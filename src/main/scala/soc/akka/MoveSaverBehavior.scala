package soc.akka

import akka.actor.typed.scaladsl.Behaviors
import soc.sql.MoveEntry
import soc.storage.MoveSaver

case class DoMove()

object MoveSaverBehavior {

  def moveSaverBehavior(saver: MoveSaver) = Behaviors.receiveMessage[MoveEntry] {
    case entry: MoveEntry =>
      saver.saveMove(entry)
      Behaviors.same
  }

}
