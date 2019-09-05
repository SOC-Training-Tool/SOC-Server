package soc.simulation

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.Scheduler
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import soc.akka.MoveResultProviderMessage.MoveResultProviderMessage
import soc.akka.messages.{GameMessage, StateMessage}
import soc.game.{GameConfiguration, GameRules}
import soc.game.board.{BoardConfiguration, BoardGenerator}
import soc.game.inventory.{Inventory, InventoryHelperFactory}
import soc.playerRepository.PlayerRepository
import soc.simulation.SimulationController.{GetGameIds, QueueGamesMessage, SimulationQueueMessage}
import scala.concurrent.duration._

import collection.mutable.{Map => MutMap, Queue}
import scala.util.Random

class SimulationController[GAME <: Inventory[GAME]](
  val playerRepository: PlayerRepository[GAME],
  val resultProvider: ActorRef[MoveResultProviderMessage[GAME]],
  val moveRecorder: Option[ActorRef[GameMessage]],
  implicit val rules: GameRules = GameRules.default,
  private var _gamesAtATime: Int =  SimulationController.DEFAULT_GAMES_AT_A_TIME
) (implicit val timeout: Timeout, scheduler: Scheduler) {


  def setGamesAtATime(gamesAtATime: Int) = _gamesAtATime = gamesAtATime
  def getGamesAtATime = _gamesAtATime

  val simulationQueueActor: ActorRef[SimulationQueueMessage] = ActorSystem(SimulationController.simulationQueueBehavior, "simulationQueue")


  var gamesQueued = 0

  def getCurrentGameIds = simulationQueueActor.ask[Seq[Int]](ref => GetGameIds(ref))
  def getGameStatus(gameId: Int) = ()
  def getGameDetails(gameId: Int) = ()

  def queueGames[PLAYERS <: Inventory[PLAYERS], BOARD <: BoardConfiguration](
    playerIds: Map[Int, String],
    numRandomBoards: Int,
    gamesPerBoard: Int = SimulationController.DEFAULT_GAMES_PER_BOARD)
    (implicit gameInventoryHelperFactory: InventoryHelperFactory[GAME],
      playersInventoryHelperFactory: InventoryHelperFactory[PLAYERS],
      boardGenerator: BoardGenerator[BOARD],
    random: Random
  ): Unit = {
    val boardsConfigs = (1 to numRandomBoards).map(_ => boardGenerator.randomBoard).toList
    queueGames(playerIds, boardsConfigs, gamesPerBoard)
  }

  def queueGames[PLAYERS <: Inventory[PLAYERS], BOARD <: BoardConfiguration](
    playerIds: Map[Int, String],
    boardConfigurations: List[BOARD],
    gamesPerBoard: Int = SimulationController.DEFAULT_GAMES_PER_BOARD)
    (implicit gameInventoryHelperFactory: InventoryHelperFactory[GAME],
      playersInventoryHelperFactory: InventoryHelperFactory[PLAYERS],
      boardGenerator: BoardGenerator[BOARD])
  : Unit = {

    val playerContexts = playerIds.mapValues(playerRepository.getPlayer)
    val gameConfigs: Iterator[GameConfiguration[GAME, PLAYERS, BOARD]] = boardConfigurations.zipWithIndex.toIterator.flatMap { case (board, index) =>
      (1 to gamesPerBoard).map { id =>
        val gameId = gamesQueued + (index * gamesPerBoard) + id
        GameConfiguration[GAME, PLAYERS, BOARD](gameId, board, playerContexts, resultProvider, moveRecorder, rules)
      }
    }
    gamesQueued += (gamesPerBoard * boardConfigurations.length)
    simulationQueueActor ! QueueGamesMessage(gameConfigs)
  }
}

object SimulationController {

  val DEFAULT_GAMES_PER_BOARD = 100
  val DEFAULT_GAMES_AT_A_TIME = 100



  def simulationQueueBehavior = Behaviors.withTimers { timer =>

    val gameQueue: Queue[GameConfiguration[_, _, _]] = Queue.empty
    val currentSimulations: MutMap[Int, ActorRef[StateMessage[_, _]]] = MutMap.empty

    timer.startPeriodicTimer("updateQueue", UpdateSimulations(), 1 milli)

    Behaviors.setup[SimulationQueueMessage] { context =>
      Behaviors.receiveMessage {
        case GetGameIds(ref) =>
          ref ! currentSimulations.keys.toSeq
          Behaviors.same

        case QueueGamesMessage(games) =>
          games.foreach (gameQueue.enqueue(_))
          Behaviors.same

        case UpdateSimulations =>
          currentSimulations.filter { case(id, actor) =>

          }

          Behaviors.same

      }

    }

  }
  trait SimulationQueueMessage

  case class QueueGamesMessage(games: Iterator[GameConfiguration[_, _, _]]) extends SimulationQueueMessage
  case class GetGameIds(respondTo: ActorRef[Seq[Int]]) extends SimulationQueueMessage
  case object UpdateSimulations extends SimulationQueueMessage

}