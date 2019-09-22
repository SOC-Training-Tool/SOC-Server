package soc.playerRepository

import io.grpc.stub.StreamObserver
import soc.akka.messages.RequestMessage
import soc.akka.messages.RequestMessage.{DiscardCardRequest, InitialPlacementRequest, MoveRequest, MoveRobberRequest}
import soc.game.{CatanMove, GameState, MoveResult}
import soc.game.inventory.Inventory
import soc.game.inventory.Inventory.PerfectInfo
import soc.game.player.moveSelector.PossibleMoveSelector
import soc.protos.game.GameUpdate
import soc.storage.GameId

import scala.collection.mutable.HashMap
import scala.concurrent.{Future, Promise}
import scala.util.Random

class PlayerContext[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER]](val name: String) {

  val randomMoveSelector = PossibleMoveSelector.randSelector[PLAYER](new Random)

  private[this] val gameObservers: HashMap[(String, Int), StreamObserver[GameUpdate]] = HashMap.empty
  private[this] val expectedResponses: HashMap[(String, Int), Promise[CatanMove]] = HashMap.empty
  private [this] var lastRequest: HashMap[(String, Int), RequestMessage[GAME, PLAYER]] = HashMap.empty

  def numGames = gameObservers.size

  def addObserver(gameId: String, position: Int, observer: StreamObserver[GameUpdate]): PlayerContext[GAME, PLAYER] = {
    gameObservers.put((gameId, position), observer)
    this
  }

  def sendInitialGameState(gameId: GameId, position: Int, state: GameState[PLAYER]): Unit = {
    val observer: StreamObserver[GameUpdate] = gameObservers.getOrElse((gameId.key, position), throw new Exception(""))
    //observer.onNext(new GameUpdate(Nil, moveResult.toString, position))
  }

  def sendGameOver(gameId: GameId, position: Int, msg: String) {
    val observer: StreamObserver[GameUpdate] = gameObservers.getOrElse((gameId.key, position), throw new Exception(""))
    observer.onNext(GameUpdate(payload = "GAME OVER: " + msg, actionRequestedPlayers = Seq.empty[String]))
  }

  def updateGameState(gameId: GameId, position: Int, moveResult: MoveResult): Unit = {
    val observer: StreamObserver[GameUpdate] = gameObservers.getOrElse((gameId.key, position), throw new Exception(""))
    observer.onNext(new GameUpdate(Nil, moveResult.toString, position))
  }

  def getMoveResponse(request: RequestMessage[GAME, PLAYER]): Future[CatanMove] = this.synchronized {
    val playerKey = (request.gameId.key, request.playerId)
    lastRequest.put(playerKey, request)

    val observer: StreamObserver[GameUpdate] = gameObservers.getOrElse(playerKey, throw new Exception(""))
    // TODO pass state: request.toString
    observer.onNext(new GameUpdate(Seq(name), request.getClass().toString().split("\\$")(1), request.playerId))

    val responsePromise = Promise.apply[CatanMove]()
    expectedResponses.put(playerKey, responsePromise)
    responsePromise.future
  }

  def receiveMove(gameId: String, position: Int, move: CatanMove): Boolean = {
    expectedResponses.get((gameId, position)).map(_.success(move))
    val p = expectedResponses.remove((gameId, position))
    p match {
      case Some(p) => true
      case None => false
    }
  }

  def getLastRequestRandomMove(gameId: String, position: Int): CatanMove = this.synchronized {
    val request = lastRequest.getOrElse((gameId, position), null)
    request match {
      case InitialPlacementRequest(gameId, state, inventory: PerfectInfo, position, first, _) =>
        randomMoveSelector.initialPlacementMove(state, inventory, position)(first)
      case DiscardCardRequest(gameId, state: GameState[PLAYER], inventory: PerfectInfo, position, _) =>
        randomMoveSelector.discardCardsMove(state, inventory, position)
      case MoveRobberRequest(gameId, state: GameState[PLAYER], inventory: PerfectInfo, position, _) =>
        randomMoveSelector.moveRobberAndStealMove(state, inventory, position)
      case MoveRequest(gameId, state: GameState[PLAYER], inventory: PerfectInfo, position, _) =>
        randomMoveSelector.turnMove(state, inventory, position)
      case _ =>
        null
    }
  }

//  def getInitialPlacementMove(gameId: GameId, position: Int, inventory: GAME, id: Int, first: Boolean): Future[CatanMove] = {
//     moveSelector.initialPlacementMove(getGameState(gameId, position), inventory, id)(first)
//  }
//
//  def getDiscardCardMove(gameId: GameId, position: Int, inventory: GAME, id: Int): Future[CatanMove] = {
//    moveSelector.discardCardsMove(getGameState(gameId, position), inventory, id)
//  }
//
//  def getMoveRobberAndStealMove(gameId: GameId, position: Int, inventory: GAME, id: Int): Future[CatanMove] = {
//    moveSelector.moveRobberAndStealMove(getGameState(gameId, position), inventory, id)
//  }
//
//  def getMove(gameId: GameId, position: Int, inventory: GAME, id: Int): Future[CatanMove] = {
//    moveSelector.turnMove(getGameState(gameId, position), inventory, id)
//  }

  //  def getGameState(gameId: GameId, position: Int): GameState[T] = this.synchronized(cache((gameId, position)))
  //  def removeGameState(gameId: GameId, position: Int): Option[GameState[T]] = this.synchronized{cache.remove((gameId, position))}
  //  def numGames = cache.size
  //  def gameIds = cache.keys.toSeq
}



