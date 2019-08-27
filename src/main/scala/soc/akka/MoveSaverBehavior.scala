package soc.akka

import akka.actor.typed.scaladsl.Behaviors
import soc.akka.messages.{GameMessage, MoveEntryMessage, SaveGameMessage, Terminate}
import soc.game.board.BoardConfiguration
import soc.storage.{MoveEntry, MoveSaver}

object MoveSaverBehavior {

  def moveSaverBehavior[BOARD <: BoardConfiguration](saver: MoveSaver[BOARD]) = Behaviors.receiveMessage[GameMessage] {

    case MoveEntryMessage(entry) =>
      saver.saveMove(entry)
      Behaviors.same

    case SaveGameMessage(gameId, board, players) =>
      saver.saveGame(gameId, board.asInstanceOf[BOARD], players)
      Behaviors.same

    case Terminate =>
      Behaviors.stopped
  }

}
