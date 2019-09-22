package soc.playerRepository

import io.grpc.stub.StreamObserver
import soc.akka.messages.RequestMessage
import soc.akka.messages.RequestMessage.{DiscardCardRequest, InitialPlacementRequest, MoveRequest, MoveRobberRequest}
import soc.game.{CatanMove, GameState, MoveResult}
import soc.game.inventory.Inventory
import soc.game.inventory.Inventory.PerfectInfo
import soc.game.player.moveSelector.PossibleMoveSelector
import soc.protos.game.{GameMessage, ActionRequest, GameEvent, GameAction}
import soc.storage.GameId

import scala.collection.mutable.HashMap
import scala.concurrent.{Future, Promise}
import scala.util.Random
import soc.game.BuildCityMove
import _root_.soc.protos.game.ActionSpecification
import soc.game.BuildSettlementMove
import soc.game.RollResult
import _root_.soc.protos.game.ActionResult
import soc.game.MoveRobberAndStealResult
import soc.game.BuildRoadMove
import soc.game.BuyDevelopmentCardResult
import soc.game.KnightResult
import soc.game.YearOfPlentyMove
import soc.game.MonopolyResult
import soc.game.RoadBuilderMove
import soc.game.PortTradeMove
import soc.game.EndTurnMove
import soc.game.DiscardResourcesMove
import soc.game.InitialPlacementMove

class PlayerContext[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER]](val name: String) {

  val randomMoveSelector = PossibleMoveSelector.randSelector[PLAYER](new Random)

  private[this] val gameObservers: HashMap[(String, Int), StreamObserver[GameMessage]] = HashMap.empty
  private[this] val expectedResponses: HashMap[(String, Int), Promise[CatanMove]] = HashMap.empty
  private [this] var lastRequest: HashMap[(String, Int), RequestMessage[GAME, PLAYER]] = HashMap.empty

  def numGames = gameObservers.size

  def addObserver(gameId: String, position: Int, observer: StreamObserver[GameMessage]): PlayerContext[GAME, PLAYER] = {
    gameObservers.put((gameId, position), observer)
    this
  }

  def sendInitialGameState(gameId: GameId, position: Int, state: GameState[PLAYER]): Unit = {
    val observer: StreamObserver[GameMessage] = gameObservers.getOrElse((gameId.key, position), throw new Exception(""))
    //observer.onNext(new GameMessage(Nil, moveResult.toString, position))
  }

  def sendGameOver(gameId: GameId, position: Int, msg: String) {
    val observer: StreamObserver[GameMessage] = gameObservers.getOrElse((gameId.key, position), throw new Exception(""))
    observer.onNext(GameMessage().withEvent(new GameEvent(message = "GAME OVER")))
  }

  def updateGameState(gameId: GameId, position: Int, moveResult: MoveResult): Unit = {
    val observer: StreamObserver[GameMessage] = gameObservers.getOrElse((gameId.key, position), throw new Exception(""))
    var event = getEvent(position, moveResult)
    observer.onNext(new GameMessage().withEvent(event));
  }

  def getMoveResponse(request: RequestMessage[GAME, PLAYER]): Future[CatanMove] = this.synchronized {
    val playerKey = (request.gameId.key, request.playerId)
    lastRequest.put(playerKey, request)

    val observer: StreamObserver[GameMessage] = gameObservers.getOrElse(playerKey, throw new Exception(""))
    // TODO pass state: request.toString
    //observer.onNext(new GameMessage(Seq(name), request.getClass().toString().split("\\$")(1), request.playerId))

    val responsePromise = Promise.apply[CatanMove]()
    expectedResponses.put(playerKey, responsePromise)
    responsePromise.future
  }

  def receiveMove(gameId: String, position: Int, move: CatanMove): Boolean = {
    expectedResponses.get((gameId, position)).map(_.success(move))
    expectedResponses.remove((gameId, position)).isDefined
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

  def getEvent(position: Int, moveResult: MoveResult): GameEvent = {
    moveResult match {
      case InitialPlacementMove(first, settlement, road) => 
        null
      case RollResult(roll) => 
        GameEvent(position, GameAction.ROLL_DICE, null, Some(ActionResult(roll=roll.number)))
      case MoveRobberAndStealResult(robberLocation, steal) => 
        val s = steal.getOrElse(null)
        GameEvent(position, GameAction.MOVE_ROBBER_AND_STEAL, Some(ActionSpecification(hex=robberLocation.toString(), otherPlayerPosition=s.victim)))
      case BuildRoadMove(edge) => 
        null
      case BuildSettlementMove(vertext) =>
        null
      case BuildCityMove(vertex) =>
        GameEvent(position, GameAction.BUILD_CITY, Some(ActionSpecification(vertex=vertex.node.toString())))
      case BuyDevelopmentCardResult(card) => 
        null
      case KnightResult(robber) => 
        null
      case RoadBuilderMove(road1, road2) => 
        null
      case YearOfPlentyMove(res1, res2) => 
        null
      case MonopolyResult(cardsLost) => 
        null
      case PortTradeMove(give, get) => 
        null
      case DiscardResourcesMove(resourceSet) => 
        null
      case EndTurnMove => 
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



