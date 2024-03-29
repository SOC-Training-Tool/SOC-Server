package soc.behaviors

import akka.actor.typed.scaladsl.Behaviors
import soc.behaviors.messages.{GameMessage, MoveEntryMessage, SaveGameMessage, Terminate}
import soc.board.BoardConfiguration
import soc.storage.{GameId, MoveEntry, MoveSaver}
import scala.language.postfixOps

import scala.concurrent.duration._

object MoveSaverBehavior {

  case class GameToSave[BOARD <: BoardConfiguration](gameId: GameId, boardConfiguration: BOARD, players: Map[(String, Int), Int])

  object SendGame extends GameMessage

  def moveSaverBehavior[BOARD <: BoardConfiguration](saver: MoveSaver[BOARD]) = Behaviors.withTimers[GameMessage] { scheduler =>

    var gamesToSave: List[GameToSave[BOARD]] = Nil
    var toTerminate: Boolean = false

    scheduler.startPeriodicTimer("timer", SendGame, 1500 millis )

    Behaviors.setup { context =>
      Behaviors.receiveMessage[GameMessage] {

        case MoveEntryMessage(entry) =>
          saver.saveMove(entry)
          Behaviors.same

        case SaveGameMessage(gameId, board, players) =>
          gamesToSave = gamesToSave ::: List(GameToSave(gameId, board.asInstanceOf[BOARD], players))
          Behaviors.same

        case SendGame =>
          gamesToSave.headOption.fold {
            if (toTerminate) Behaviors.stopped[GameMessage]
            else Behaviors.same[GameMessage]
          } { g =>
            context.log.info(s"Saving Game ${g.gameId}")
            saver.saveGame(g.gameId, g.boardConfiguration, g.players)
            gamesToSave = gamesToSave.tail
            Behaviors.same[GameMessage]
          }

        case Terminate =>
          toTerminate = true
          Behaviors.same
      }
    }
  }
}
