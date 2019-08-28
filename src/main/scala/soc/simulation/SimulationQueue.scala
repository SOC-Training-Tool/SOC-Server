package soc.simulation

import akka.actor.typed.{ActorRef, ActorSystem}
import soc.akka.Main.{moveSaverActor, players}
import soc.akka.{GameBehavior, MoveResultProvider}
import soc.akka.messages.{GameMessage, Terminate}
import soc.game.{GameConfiguration, GameRules}
import soc.game.board.{BoardConfiguration, BoardGenerator}
import soc.game.inventory.{Inventory, InventoryManagerFactory}

import scala.concurrent.Future
import scala.util.Random

class SimulationQueue[GAME <: Inventory[GAME], PLAYERS <: Inventory[PLAYERS], BOARD <: BoardConfiguration](
  players: Map[(String, Int), ActorRef[GameMessage]],
  resultProvider: MoveResultProvider[GAME],
  moveRecorder: Option[ActorRef[GameMessage]],
  rules: GameRules,
  boardConfigurations: List[BOARD],
  gamesPerBoard: Int,
  gamesAtATime: Int
)(implicit
  gameInventoryManagerFactory: InventoryManagerFactory[GAME],
  playersInventoryManagerFactory: InventoryManagerFactory[PLAYERS],
  boardGenerator: BoardGenerator[BOARD]) {

  val gameConfigs: Iterator[GameConfiguration[GAME, PLAYERS, BOARD]] = boardConfigurations.zipWithIndex.toIterator.flatMap { case (board, index) =>
    (1 to gamesPerBoard).map { id =>
      val gameId = (index * gamesPerBoard) + id
      GameConfiguration[GAME, PLAYERS, BOARD](gameId, board, players, resultProvider, moveRecorder, rules)
    }
  }

  var currentSimulations: List[Future[_]] = Nil

  def startGames: Unit = {
    playNextNSimulations(gamesAtATime)
    while (!currentSimulations.isEmpty) {
      currentSimulations = currentSimulations.filterNot(_.isCompleted)
      playNextNSimulations(gamesAtATime - currentSimulations.length)
    }
    players.values.foreach(_ ! Terminate)
    moveSaverActor ! Terminate
  }

  private def playNextNSimulations(n: Int): Unit = {
    (1 to n).foreach { _ => playNextSimulation}
  }

  private def playNextSimulation: Unit =  if (gameConfigs.hasNext) {
    val config = gameConfigs.next()
    currentSimulations = ActorSystem(GameBehavior.gameBehavior(config), s"SettlersOfCatan${config.gameId}").whenTerminated :: currentSimulations
  }
}

object SimulationQueue {

  def apply[GAME <: Inventory[GAME], PLAYERS <: Inventory[PLAYERS], BOARD <: BoardConfiguration](
    players: Map[(String, Int), ActorRef[GameMessage]],
    resultProvider: MoveResultProvider[GAME],
    moveRecorder: Option[ActorRef[GameMessage]],
    rules: GameRules,
    numBoard: Int,
    gamesPerBoard: Int,
    gamesAtATime: Int
  )(implicit
    gameInventoryManagerFactory: InventoryManagerFactory[GAME],
    playersInventoryManagerFactory: InventoryManagerFactory[PLAYERS],
    boardGenerator: BoardGenerator[BOARD],
    random: Random
  ): SimulationQueue[GAME, PLAYERS, BOARD] = {

    val boards = (1 to numBoard).map(_ => boardGenerator.randomBoard).toList
    new SimulationQueue[GAME, PLAYERS, BOARD](players, resultProvider, moveRecorder, rules, boards, gamesPerBoard, gamesAtATime)
  }

}