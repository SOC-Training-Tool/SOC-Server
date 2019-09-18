package server

import akka.actor.typed.{ActorRef, ActorSystem}
import io.grpc.stub.StreamObserver
import soc.akka.GameBehavior
import soc.akka.MoveResultProviderMessage.MoveResultProviderMessage
import soc.akka.messages.{GameMessage, StateMessage}
import soc.game.{GameConfiguration, GameRules, GameState}
import soc.game.board.{BaseBoardConfiguration, BaseCatanBoard, BoardConfiguration, BoardGenerator}
import soc.game.inventory.{Inventory, InventoryHelperFactory}
import soc.game.inventory.Inventory.{NoInfo, PerfectInfo}
import soc.playerRepository.{PlayerContext, PlayerRepository}
import soc.protos.game.GameUpdate
import soc.storage.GameId

import scala.collection.mutable.HashMap
import scala.util.Random

class GameContext[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER], BOARD <: BoardConfiguration](gameConfiguration: GameConfiguration[GAME, PLAYER, BOARD]) {

  val gameId = gameConfiguration.gameId
  val players = gameConfiguration.players
  val system = ActorSystem(GameBehavior.gameBehavior(gameConfiguration), s"SOC-Game-${gameConfiguration.gameId.key}")

  def getPlayer(position: Int): Option[PlayerContext[GAME, PLAYER]] = players.get(position)

  def isFinished: Boolean = system.whenTerminated.isCompleted

  def getGameState: GameState[GAME] = null
  def getGameStatePlayerPerspective(playerPosition: Int): GameState[PLAYER] = null
  def getGameStatePlayerPerspective(playerId: String): GameState[PLAYER] = null


//
//
//  //  private var game: ActorSystem[StateMessage[PerfectInfo, NoInfo]] = null
//  private val subscribers: HashMap[String, StreamObserver[GameUpdate]] = new HashMap()
//
//
//  def start(): Unit = {
//    val config = GameConfiguration[PerfectInfo, NoInfo, BaseBoardConfiguration](gameId.toInt, boardConfig, players, randomMoveResultProvider, None, gameRules)
//    // TODO: Hold onto a reference to this game / future
//    // This will allow us to query a game status (ie, what is the current turn), save it, terminate it, etc.
//    val future = ActorSystem(GameBehavior.gameBehavior(config, subscribers), s"SettlersOfCatan${gameId}").whenTerminated
//    // TODO: Clean up the game from the hashtable of currently running games
//    //future onComplete {
//    //  case Success() =>
//    //  case Failure(t) => println("An error has occurred: " + t.getMessage)
//    //}
//  }
//
//  def subscribe(name: String, observer: StreamObserver[GameUpdate]): Unit = {
//    subscribers += (name -> observer)
//  }
}


case class GameBuilder[GAME <: Inventory[GAME], PLAYERS <: Inventory[PLAYERS], BOARD <: BoardConfiguration](
  gameId: GameId,
  boardConfig: BOARD,
  moveResultProvider: ActorRef[MoveResultProviderMessage[GAME]],
  moveRecorder: Option[ActorRef[GameMessage]],
  gameRules: GameRules,
  numPlayers: Int,
  subscribers: Map[String, (Option[Int], StreamObserver[GameUpdate])] = Map.empty)
  (implicit
    playerRepo: PlayerRepository,
    random: Random,
    gameInventoryHelperFactory: InventoryHelperFactory[GAME],
    playersInventoryHelperFactory: InventoryHelperFactory[PLAYERS],
    boardGenerator: BoardGenerator[BOARD]) {

  def subscribePlayer(playerId: String, position: Option[Int], observer: StreamObserver[GameUpdate]): GameBuilder[GAME, PLAYERS, BOARD] = copy(subscribers = subscribers + (playerId -> (position, observer)))

  def start: GameContext[GAME, PLAYERS, BOARD] = {
    if (subscribers.keys.size < numPlayers) {
      // THROW EXCEPTION
    }

    val players = {
      val positions = 0 to (numPlayers - 1)
      val selectedPositions = subscribers.filter(_._2._1.isDefined).values.map(_._1.get).toSeq
      var nonSelectedPositions = random.shuffle(positions.filter(selectedPositions.contains)).toList
      subscribers.map {
        case (playerId, (Some(position), observer)) =>
          position -> playerRepo.getPlayer(playerId)
            .getOrElse(throw new Exception(""))
            .asInstanceOf[PlayerContext[GAME, PLAYERS]]
            .addObserver(gameId.key, position, observer)
        case (playerId, (None, observer)) =>
          val position = nonSelectedPositions.head
          nonSelectedPositions = nonSelectedPositions.tail
          position -> playerRepo.getPlayer(playerId)
            .getOrElse(throw new Exception(""))
            .asInstanceOf[PlayerContext[GAME, PLAYERS]]
            .addObserver(gameId.key, position, observer)
      }
    }
    val config = GameConfiguration(gameId, boardConfig, players, moveResultProvider, moveRecorder, gameRules)
    new GameContext(config)
  }

}

object GameContext {

  def builder[GAME <: Inventory[GAME], PLAYERS <: Inventory[PLAYERS], BOARD <: BoardConfiguration](
    gameId: GameId,
    boardConfig: BOARD,
    moveResultProvider: ActorRef[MoveResultProviderMessage[GAME]],
    moveRecorder: Option[ActorRef[GameMessage]],
    gameRules: GameRules,
    numPlayers: Int = 4)
    (implicit playerRepo: PlayerRepository,
      random: Random,
      gameInventoryHelperFactory: InventoryHelperFactory[GAME],
      playersInventoryHelperFactory: InventoryHelperFactory[PLAYERS],
      boardGenerator: BoardGenerator[BOARD]): GameBuilder[GAME, PLAYERS, BOARD] = {
      new GameBuilder(gameId, boardConfig, moveResultProvider, moveRecorder, gameRules, numPlayers)
  }

}