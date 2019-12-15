package server

import java.util.logging.Logger

import akka.actor.typed.ActorSystem
import io.grpc.stub.StreamObserver
import protos.soc.game.GameStatus._
import soc.GameConfiguration
import soc.behaviors.{GameBehavior, MoveResultProvider}
import soc.board.{BoardConfiguration, BoardGenerator, CatanBoard}
import soc.core.GameRules
import soc.inventory.{Inventory, InventoryHelperFactory}
import soc.playerRepository.{PlayerContext, ReceiveMoveFromClient}
import protos.soc.game.{GameMessage, GameResults, GameStatus}
import protos.soc.game.SubscribeRequest.SubscriptionType
import protos.soc.game.SubscribeRequest.SubscriptionType.{OBSERVER, PLAYER}
import soc.state.{GamePhase, GameState}
import soc.storage.GameId
import protocoder.ProtoCoder.ops._
import protocoder.implicits.StateProto._
import protocoder.implicits.MoveProto._
import soc.behaviors.GameBehavior.Start
import soc.moves.{CatanMove, MoveResult}
import protocoder.implicits.MoveProto._
import protocoder.implicits.BoardProto._
import protocoder.ProtoCoder._
import soc.board.BaseCatanBoard.baseBoardMapping
import protocoder.ProtoCoder.ops._
import protos.soc.game.GameResults.ActionResultContainer
import protos.soc.moves.ActionResult

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.Random

class GameContext[GAME <: Inventory[GAME], BOARD <: BoardConfiguration](
  val controller: GameController[GAME, BOARD],
  val gameId: GameId,
  val boardConfiguration: BOARD,
  val moveResultProvider: MoveResultProvider[GAME],
  val numPlayers: Int,
  //moveRecorder: Option[ActorRef[AkkaGameMessage]],
  val gameRules: GameRules,
  val saveGame: Boolean,
  val logger: Logger)
  (implicit val gameInventoryHelperFactory: InventoryHelperFactory[GAME],
                boardGenerator: BoardGenerator[BOARD],
                random: Random) {

  private val positions: Seq[Int] = (0 to numPlayers - 1)

  val players: mutable.HashMap[Int, PlayerContext[GAME]] = mutable.HashMap.empty
  val observers: mutable.HashMap[String, PlayerContext[GAME]] = mutable.HashMap.empty
  val moveList = mutable.ListBuffer.empty[ActionResult]
  var initialBoard: CatanBoard = _
  var gameState: GameState[GAME] = _
  var status: GameStatus = WAITING_FOR_PLAYERS

  def getPlayer(position: Int): Option[PlayerContext[GAME]] = players.get(position)

  //def playerAction(pos: Int, request: RequestMessage): Future[ReceiveMoveFromClient] = players(pos).getMoveResponse(request)
  def getNextAction: List[Future[ReceiveMoveFromClient]] = {
    val currentPhase = getGameState.phase
    val publicGameState = getGameState.toPublicGameState

    val playersToAction = {
      if (currentPhase == GamePhase.Discard) getGameState.expectingDiscard
      else List(getGameState.currentPlayer)
    }

    playersToAction.map { pos =>
      val inventory = getGameState.players.getPlayer(pos).inventory
      players(pos).getMoveResponse((currentPhase, publicGameState, inventory).proto)
    }
  }

  def canDoMove(move: CatanMove): Boolean = true

  def updateGameState(id: Int, moveResult: MoveResult): Unit = {
    val transition = getGameState.apply(moveResult)
    moveList += (moveResult, transition.transactions).proto
    gameState = transition.state
    moveResult.getPerspectiveResults(players.keys.toSeq).toSeq.foreach { case (id, result: MoveResult) =>
      players(id).updateGameState(gameId, id, (result, transition.transactions).proto)
    }
  }

  def getGameState: GameState[GAME] = gameState

  private def subscribePlayer(playerId: String, position: Option[Int], observer: StreamObserver[GameMessage]): Unit = this.synchronized {
    position.fold {
      val selectedPositions = players.keys.toSeq
      val nonSelectedPositions = random.shuffle(positions.filterNot(selectedPositions.contains)).toList
      val pos = nonSelectedPositions.head
      players.put(pos, PlayerContext(playerId, gameId, pos, observer))
    }(pos => players.put(pos, PlayerContext(playerId, gameId, pos, observer)))
  }

  private def subscribeObserver(observerId: String, observer: StreamObserver[GameMessage]): Unit = this.synchronized {
    observers.put(observerId, PlayerContext(observerId, gameId, observer))
  }

  def canStart= players.size == numPlayers

  def canSubscribe(subscriptionType: SubscriptionType, position: Option[Int] = None): Boolean = subscriptionType match {
    case PLAYER => (players.size < numPlayers) && position.fold(true)(p => !players.keys.toSeq.contains(p) && positions.contains(p))
    case OBSERVER => true
  }

  def subscribe(subscriptionType: SubscriptionType, id: String, position: Option[Int], observer: StreamObserver[GameMessage]) = this.synchronized {
    subscriptionType match {
      case OBSERVER => subscribeObserver(id, observer)
      case PLAYER => subscribePlayer(id, position, observer)
    }
  }

  def start: Unit = {
    if (players.size < numPlayers) {
      throw new Exception(s"Not enough players for game. players subscribed: ${players.size}, required: $numPlayers")
    }
    if (status != WAITING_FOR_PLAYERS) {
      throw new Exception("cannot start game more than once")

    }

    val gameConfiguration: GameConfiguration[GAME, BOARD] = GameConfiguration[GAME, BOARD](gameId, boardConfiguration, players.view.mapValues(_.name).toMap, gameRules)
    val system: ActorSystem[GameBehavior.Behavior2Messages] = ActorSystem(GameBehavior.gameBehavior2(gameConfiguration, this, moveResultProvider), s"SOC-Game-${gameConfiguration.gameId.key}")

    initialBoard = gameConfiguration.initBoard
    gameState = gameConfiguration.initState

    status = PLAYING
    system ! Start
  }

  def onComplete: Unit = {
    val state = getGameState
    val winner = state.players.getPlayers.find(_.points >= 10)
    val winMsg = s"Player ${winner.get.position} has won game ${gameId} with ${winner.get.points} points and ${state.turn} rolls ${state.players.getPlayers.map(p => (p.position, p.points))}"
    println(winMsg)
    status = FINISHED
    if (saveGame) {
      controller.saveGame(gameResults)
    }
  }

  private def gameResults: GameResults = {
    val moveResults = moveList.toSeq
    GameResults.of(
      gameId.key,
      initialBoard.proto,
      ActionResultContainer(moveResults)
    )
  }
}

object GameContext {
  def apply[GAME <: Inventory[GAME], BOARD <: BoardConfiguration](
    controller: GameController[GAME, BOARD],
    gameId: GameId,
    boardConfiguration: BOARD,
    moveResultProvider: MoveResultProvider[GAME],
    numPlayers: Int,
    gameRules: GameRules,
    saveGame: Boolean,
    logger: Logger
  )
  (implicit gameInventoryHelperFactory: InventoryHelperFactory[GAME], boardGenerator: BoardGenerator[BOARD], random: Random): GameContext[GAME,  BOARD] = {
    new GameContext[GAME, BOARD](controller, gameId, boardConfiguration, moveResultProvider, numPlayers, gameRules, saveGame, logger)
  }

  def builder: Unit = ()
  def loadFromDataBase(gameId: GameId): Unit = ()

}