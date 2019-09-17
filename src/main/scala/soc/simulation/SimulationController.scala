package soc.simulation

import akka.Done
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.Scheduler
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import soc.akka.GameBehavior
import soc.akka.MoveResultProviderMessage.MoveResultProviderMessage
import soc.akka.messages.{GameMessage, StateMessage}
import soc.game.{GameConfiguration, GameRules}
import soc.game.board.{BoardConfiguration, BoardGenerator}
import soc.game.inventory.{Inventory, InventoryHelperFactory}
import soc.playerRepository.PlayerRepository
import soc.simulation.SimulationController._
import soc.storage.{GameId, SimulatedGame}

import scala.concurrent.duration._
import collection.mutable.{Queue, Map => MutMap}
import scala.concurrent.Future
import scala.util.Random

class SimulationController[GAME <: Inventory[GAME], PLAYERS <: Inventory[PLAYERS], BOARD <: BoardConfiguration](
  val playerRepository: PlayerRepository[GAME],
  val resultProvider: ActorRef[MoveResultProviderMessage[GAME]],
  val moveRecorder: Option[ActorRef[GameMessage]],
  implicit val rules: GameRules = GameRules.default,
  private var _gamesAtATime: Int = SimulationController.DEFAULT_GAMES_AT_A_TIME
) {

  val gameQueue: Queue[GameConfiguration[GAME, PLAYERS, BOARD]] = Queue.empty
  val currentSimulations: MutMap[GameId, ActorSystem[StateMessage[GAME, PLAYERS]]] = MutMap.empty

  val simulationQueueActor = ActorSystem(simulationQueueBehavior(_gamesAtATime), "simulationQueue")

  implicit val timeout: Timeout = Timeout(1 seconds)
  implicit val scheduler: Scheduler = simulationQueueActor.scheduler

  def setGamesAtATime(gamesAtATime: Int) = _gamesAtATime = gamesAtATime

  def getGamesAtATime = _gamesAtATime

  def getCurrentGameIds = simulationQueueActor.ask[Seq[GameId]](ref => GetGameIds(ref))

  def getGameStatus(gameId: Int) = ()

  def getGameDetails(gameId: Int) = ()

  def run: Future[Done] = {
    simulationQueueActor ! RunSimulations
    simulationQueueActor.whenTerminated
  }
  def pause: Unit = simulationQueueActor ! PauseSimulations
  def terminateWhenFinished: Unit = simulationQueueActor ! TerminateWhenFinished

  def queueGamesRandomBoard(
    simulationBatchId: String,
    playerIds: Map[Int, String],
    numRandomBoards: Int,
    gamesPerBoard: Int = SimulationController.DEFAULT_GAMES_PER_BOARD)
    (implicit gameInventoryHelperFactory: InventoryHelperFactory[GAME],
      playersInventoryHelperFactory: InventoryHelperFactory[PLAYERS],
      boardGenerator: BoardGenerator[BOARD],
      random: Random
    ): Unit = {
    val boardsConfigs = (1 to numRandomBoards).map(_ => boardGenerator.randomBoard).toList
    queueGamesWithBoards(simulationBatchId, playerIds, boardsConfigs, gamesPerBoard)(gameInventoryHelperFactory, playersInventoryHelperFactory, boardGenerator)
  }

  def queueGamesWithBoards(
    simulationBatchId: String,
    playerIds: Map[Int, String],
    boardConfigurations: List[BOARD],
    gamesPerBoard: Int = SimulationController.DEFAULT_GAMES_PER_BOARD)
    (implicit gameInventoryHelperFactory: InventoryHelperFactory[GAME],
      playersInventoryHelperFactory: InventoryHelperFactory[PLAYERS],
      boardGenerator: BoardGenerator[BOARD])
  : Unit = {

    val playerContexts = playerIds.mapValues(playerRepository.getPlayer(_).get)
    val gameConfigs: Iterator[GameConfiguration[GAME, PLAYERS, BOARD]] = boardConfigurations.zipWithIndex.toIterator.flatMap { case (board, index) =>
      (1 to gamesPerBoard).map { id =>
        val gameId = GameId(SimulatedGame, simulationBatchId, (index * gamesPerBoard) + id)
        GameConfiguration[GAME, PLAYERS, BOARD](gameId, board, playerContexts, resultProvider, moveRecorder, rules)
      }
    }
    simulationQueueActor ! QueueGamesMessage(gameConfigs)
  }

  private def simulationQueueBehavior(gamesAtATime: Int): Behavior[SimulationQueueMessage] = Behaviors.setup { context =>

    def playNextNSimulations(n: Int): Unit = (1 to n).foreach { _ => playNextSimulation }
    def playNextSimulation: Unit = if (!gameQueue.isEmpty) {
      val config = gameQueue.dequeue()
      val actor = ActorSystem(GameBehavior.gameBehavior(config), s"SettlersOfCatan-${config.gameId.key}")
      currentSimulations(config.gameId) = actor
    }

    var terminateWhenFinished = false
    val timerKey = "updateQueue"

    Behaviors.withTimers[SimulationQueueMessage] { timer =>
      Behaviors.receiveMessage[SimulationQueueMessage] {
        case RunSimulations =>
          context.log.info("Starting Games")
          timer.startPeriodicTimer(timerKey, UpdateSimulations, 10 milli)
          Behaviors.same

        case PauseSimulations =>
          timer.cancel(timerKey)
          Behaviors.same

        case TerminateWhenFinished =>
          terminateWhenFinished = true
          if (gameQueue.isEmpty) {
            timer.cancel(timerKey)
            context.scheduleOnce(15 millis, context.self, Terminate)
            Behaviors.same
          }

          else Behaviors.same

        case GetGameIds(respondTo) =>
          respondTo ! currentSimulations.keys.toSeq
          Behaviors.same

        case QueueGamesMessage(games) =>
          val (iter1, iter2) = games.duplicate
          context.log.info(s"${iter1.length} games loaded to queue")
          iter2.map(_.asInstanceOf[GameConfiguration[GAME, PLAYERS, BOARD]]).foreach{ config =>
            gameQueue.enqueue(config)
          }

          Behaviors.same

        case UpdateSimulations =>
          val completedGames = currentSimulations.filter(_._2.whenTerminated.isCompleted).keys
          completedGames.foreach(currentSimulations.remove(_))
          playNextNSimulations(gamesAtATime - currentSimulations.size)
          if (currentSimulations.isEmpty && terminateWhenFinished) {
            timer.cancel(timerKey)
            context.scheduleOnce(15 millis, context.self, Terminate)
            Behaviors.same

          }
          else Behaviors.same

        case Terminate => Behaviors.stopped
      }
    }
  }
}

object SimulationController {

  val DEFAULT_GAMES_PER_BOARD = 100
  val DEFAULT_GAMES_AT_A_TIME = 100

  trait SimulationQueueMessage

  case class QueueGamesMessage(games: Iterator[GameConfiguration[_, _, _]]) extends SimulationQueueMessage
  case class GetGameIds(respondTo: ActorRef[Seq[GameId]]) extends SimulationQueueMessage
  case object UpdateSimulations extends SimulationQueueMessage
  case object RunSimulations extends SimulationQueueMessage
  case object PauseSimulations extends SimulationQueueMessage
  case object TerminateWhenFinished extends SimulationQueueMessage
  case object Terminate extends SimulationQueueMessage

}