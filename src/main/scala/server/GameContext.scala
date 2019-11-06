package server

import akka.actor.typed.{ActorRef, ActorSystem}
import io.grpc.stub.StreamObserver
import protos.soc.game.GameStatus.WAITING_FOR_PLAYERS
import soc.GameConfiguration
import soc.behaviors.{GameBehavior, MoveResultProvider}
import soc.behaviors.messages.{GameMessage => AkkaGameMessage}
import soc.board.{BoardConfiguration, BoardGenerator}
import soc.core.GameRules
import soc.inventory.{Inventory, InventoryHelperFactory}
import soc.playerRepository.{PlayerContext, ReceiveMoveFromClient}
import protos.soc.game.{GameMessage, GameStatus}
import protos.soc.game.SubscribeRequest.SubscriptionType
import protos.soc.game.SubscribeRequest.SubscriptionType.{OBSERVER, PLAYER}
import soc.state.{GamePhase, GameState}
import soc.storage.GameId
import protocoder.ProtoCoder.ops._
import protocoder.implicits.StateProto._
import protocoder.implicits.MoveProto._
import soc.inventory.resources.SOCTransactions
import soc.moves.{CatanMove, MoveResult}

import scala.concurrent.Future
import scala.util.Random

class GameContext[GAME <: Inventory[GAME], BOARD <: BoardConfiguration](
  gameId: GameId,
  boardConfiguration: BOARD,
  players: Map[Int, PlayerContext[GAME]],
  moveResultProvider: MoveResultProvider[GAME],
  moveRecorder: Option[ActorRef[AkkaGameMessage]],
  gameRules: GameRules)
  (implicit gameInventoryHelperFactory: InventoryHelperFactory[GAME], boardGenerator: BoardGenerator[BOARD]) {

  val gameConfiguration: GameConfiguration[GAME, BOARD] = GameConfiguration[GAME, BOARD](gameId, this, boardConfiguration, players, moveResultProvider, moveRecorder, gameRules)
  private var gameState = gameConfiguration.initState

  val system = ActorSystem(GameBehavior.gameBehavior2(gameConfiguration), s"SOC-Game-${gameConfiguration.gameId.key}")
  var status: GameStatus = WAITING_FOR_PLAYERS

  def getPlayer(position: Int): Option[PlayerContext[GAME]] = players.get(position)

  def isFinished: Boolean = system.whenTerminated.isCompleted

  //def playerAction(pos: Int, request: RequestMessage): Future[ReceiveMoveFromClient] = players(pos).getMoveResponse(request)
  def getNextAction: List[Future[ReceiveMoveFromClient]] = {
    val currentPhase = gameState.phase
    val publicGameState = gameState.toPublicGameState

    val playersToAction = {
      if (currentPhase == GamePhase.Discard) gameState.expectingDiscard
      else List(gameState.currentPlayer)
    }

    playersToAction.map { pos =>
      val inventory = gameState.players.getPlayer(pos).inventory
      players(pos).getMoveResponse((currentPhase, publicGameState, inventory).proto)
    }
  }

  def canDoMove(move: CatanMove): Boolean = true

  def updateGameState(id: Int, moveResult: MoveResult): Unit = {
    val transition = gameState.apply(moveResult)
    gameState = transition.state
    //TODO send customized MoveResult back to client
    moveResult.getPerspectiveResults(players.keys.toSeq).toSeq.foreach { case (id, result: MoveResult) =>
      players(id).updateGameState(gameId, id, (result, transition.transactions).proto)
    }
  }

  def getGameState: GameState[GAME] = gameState

  def getGameStatePlayerPerspective(playerPosition: Int): GameState[GAME] = null

  def getGameStatePlayerPerspective(playerId: String): GameState[GAME] = null
}


case class GameBuilder[GAME <: Inventory[GAME], BOARD <: BoardConfiguration](
  gameId: GameId,
  boardConfig: BOARD,
  moveResultProvider: MoveResultProvider[GAME],
  moveRecorder: Option[ActorRef[AkkaGameMessage]],
  gameRules: GameRules,
  numPlayers: Int,
  subscribers: Map[String, (Option[Int], StreamObserver[GameMessage])] = Map.empty)
  (implicit
    random: Random,
    gameInventoryHelperFactory: InventoryHelperFactory[GAME],
    boardGenerator: BoardGenerator[BOARD]) {

  lazy val chosenPositions = subscribers.values.map(_._1).toSeq

  def subscribePlayer(playerId: String, position: Option[Int], observer: StreamObserver[GameMessage]): GameBuilder[GAME, BOARD] = this.synchronized {
    copy(subscribers = (subscribers + (playerId -> (position, observer))))
  }

  def canStart: Boolean = subscribers.size == numPlayers

  def canSubscribe(subscriptionType: SubscriptionType, position: Option[Int] = None): Boolean = subscriptionType match {
    case PLAYER => (subscribers.size < numPlayers) && position.fold(true)(!chosenPositions.contains(_))
    case OBSERVER => true
  }

  def start: GameContext[GAME, BOARD] = {
    if (subscribers.keys.size < numPlayers) {
      throw new Exception(s"Not enough players for game. players subscribed: ${subscribers.keys.size}, required: $numPlayers")
    }

    val players = {
      val positions = 0 to (numPlayers - 1)
      val selectedPositions = subscribers.filter(_._2._1.isDefined).values.map(_._1.get).toSeq
      var nonSelectedPositions = random.shuffle(positions.filterNot(selectedPositions.contains)).toList
      subscribers.map[Int, PlayerContext[GAME]] {
        case (playerId, (Some(position), observer)) =>
          position -> PlayerContext(playerId, gameId, position, observer)
        case (playerId, (None, observer)) =>
          val position = nonSelectedPositions.head
          nonSelectedPositions = nonSelectedPositions.tail
          position -> PlayerContext(playerId, gameId, position, observer)
      }
    }
    GameContext(gameId, boardConfig, players, moveResultProvider, moveRecorder, gameRules)
  }

}

object GameContext {

  def builder[GAME <: Inventory[GAME], BOARD <: BoardConfiguration](
    gameId: GameId,
    boardConfig: BOARD,
    moveResultProvider: MoveResultProvider[GAME],
    moveRecorder: Option[ActorRef[AkkaGameMessage]],
    gameRules: GameRules,
    numPlayers: Int = 4)
    (implicit
      random: Random,
      gameInventoryHelperFactory: InventoryHelperFactory[GAME],
      boardGenerator: BoardGenerator[BOARD]): GameBuilder[GAME, BOARD] = {

    new GameBuilder(gameId, boardConfig, moveResultProvider, moveRecorder, gameRules, numPlayers)
  }

  def apply[GAME <: Inventory[GAME], BOARD <: BoardConfiguration](
    gameId: GameId,
    boardConfig: BOARD,
    players: Map[Int, PlayerContext[GAME]],
    moveResultProvider: MoveResultProvider[GAME],
    moveRecorder: Option[ActorRef[AkkaGameMessage]],
    gameRules: GameRules)
    (implicit gameInventoryHelperFactory: InventoryHelperFactory[GAME], boardGenerator: BoardGenerator[BOARD]): GameContext[GAME, BOARD] = {

    new GameContext[GAME, BOARD](gameId, boardConfig, players, moveResultProvider, moveRecorder, gameRules)
  }
}