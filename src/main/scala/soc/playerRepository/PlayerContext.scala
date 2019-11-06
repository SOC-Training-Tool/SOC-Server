package soc.playerRepository

import io.grpc.stub.StreamObserver
import protos.soc.game.GameMessage.Payload.{Request, Result}
import protos.soc.game.{ActionRequest, GameMessage, MoveResponse}
import protos.soc.moves.{ActionResult, GameEvent}
import protos.soc.state.Player
import soc.storage.GameId

import scala.concurrent.{Future, Promise}
import soc.moves._
import soc.inventory.Inventory

class PlayerContext[GAME <: Inventory[GAME]](
  val name: String,
  val gameId: GameId,
  val position: Int,
  val observer: StreamObserver[GameMessage]
) {

  private[this] var expectedResponses: Promise[ReceiveMoveFromClient] = _

//  def sendInitialGameState(gameId: GameId, position: Int, state: GameState[PublicInfo]): Unit = {
//    val observer: StreamObserver[GameMessage] = gameObservers.getOrElse((gameId.key, position), throw new Exception(""))
//    //observer.onNext(new GameMessage().withRequest(ActionRequest(position, ActionRequestType.ACKNOWLEDGE_START_GAME)))
//  }

//  def sendGameOver(gameId: GameId, position: Int, msg: String) {
//    val observer: StreamObserver[GameMessage] = gameObservers.getOrElse((gameId.key, position), throw new Exception(""))
//    observer.onNext(GameMessage().withEvent(new GameEvent(message = "GAME OVER")))
//  }

  def updateGameState(gameId: GameId, position: Int, actionResult: ActionResult): Unit = {
    observer.onNext(GameMessage.of(Player(name, position), Result(actionResult)))


    //val observer: StreamObserver[GameMessage] = gameObservers.getOrElse((gameId.key, position), throw new Exception(""))
    //observer.onNext(GameMessage().withEvent(event))
  }

  def getMoveResponse(actionRequest: ActionRequest): Future[ReceiveMoveFromClient] = this.synchronized {
    observer.onNext(GameMessage.of(Player(name, position), Request(actionRequest)))
    expectedResponses = Promise.apply[ReceiveMoveFromClient]()
    expectedResponses.future
  }

  def receiveMove(move: CatanMove): Future[MoveResponse] = {
    val moveReceived = ReceiveMoveFromClient(position, move)
    expectedResponses.success(moveReceived)
    moveReceived.moveResponse.future
  }
}


case class ReceiveMoveFromClient(id: Int, move: CatanMove) {
  val moveResponse = Promise.apply[MoveResponse]()
}







//  def getLastRequestRandomMove(gameId: String, position: Int): CatanMove = this.synchronized {
//    val request = lastRequest.getOrElse((gameId, position), null)
//    request match {
//      case InitialPlacementRequest(gameId, state, inventory: PerfectInfo, position, first, _) =>
//        randomMoveSelector.initialPlacementMove(state, inventory, position)(first)
//      case DiscardCardRequest(gameId, state: GameState[PLAYER], inventory: PerfectInfo, position, _) =>
//        randomMoveSelector.discardCardsMove(state, inventory, position)
//      case MoveRobberRequest(gameId, state: GameState[PLAYER], inventory: PerfectInfo, position, _) =>
//        randomMoveSelector.moveRobberAndStealMove(state, inventory, position)
//      case MoveRequest(gameId, state: GameState[PLAYER], inventory: PerfectInfo, position, _) =>
//        randomMoveSelector.turnMove(state, inventory, position)
//      case _ =>
//        null
//    }
//  }

//  def getEvent(position: Int, moveResult: MoveResult): GameEvent = {
//    moveResult match {
//      case InitialPlacementMove(first, settlement, road) =>
//        val actionSpec = ActionSpecification(vertex = settlement.node.toString(), edges = Seq(Edge(road.v1.node.toString(), road.v2.node.toString())))
//        GameEvent(position, GameAction.INITIAL_PLACEMENT, Some(actionSpec))
//      case RollResult(roll) =>
//        GameEvent(position, GameAction.ROLL_DICE, result = Some(ActionResult(roll=roll.number)))
//      case MoveRobberAndStealResult(viewableBy, robberLocation, steal) =>
//        val s = steal.getOrElse(null)
//        val op = if (s == null) -1 else s.victim
//        GameEvent(position, GameAction.MOVE_ROBBER_AND_STEAL, Some(ActionSpecification(hex = robberLocation.toString(), otherPlayerPositions = Seq(op))))
//      case BuildRoadMove(edge) =>
//          val actionSpec = ActionSpecification(edges=Seq(Edge(edge.v1.node.toString(), edge.v2.node.toString())))
//          GameEvent(position, GameAction.BUILD_ROAD, Some(actionSpec))
//      case BuildSettlementMove(vertex) =>
//        GameEvent(position, GameAction.BUILD_SETTLEMENT, Some(ActionSpecification(vertex = vertex.node.toString())))
//      case BuildCityMove(vertex) =>
//        GameEvent(position, GameAction.BUILD_CITY, Some(ActionSpecification(vertex = vertex.node.toString())))
//      case BuyDevelopmentCardResult(viewableBy, card) =>
//        GameEvent(position, GameAction.BUILD_DEVELOPMENT_CARD)
//      case KnightResult(robber) =>
//        val s = robber.steal.getOrElse(null)
//        val op = if (s == null) -1 else s.victim
//        GameEvent(position, GameAction.ACTIVATE_KNIGHT, Some(ActionSpecification(hex=robber.robberLocation.toString(), otherPlayerPositions = Seq(op))))
//      case RoadBuilderMove(road1, road2) =>
//        val r2 = road2.getOrElse(road1) // TODO FIX
//        val edges = Seq(Edge(road1.v1.node.toString(), road1.v2.node.toString()), Edge(r2.v1.node.toString(), r2.v2.node.toString()))
//        GameEvent(position, GameAction.ACTIVATE_ROAD_BUILDING, Some(ActionSpecification(edges = edges)))
//      case YearOfPlentyMove(res1, res2) =>
//        val r1 = resourceMap.getOrElse(res1.name, null)
//        val r2 = resourceMap.getOrElse(res2.name, null)
//        val actionSpec = ActionSpecification(ask=Seq(r1, r2))
//        val result = ActionResult(resourcesTransacted = Map((1, ResourceTransaction(gain = Seq(r1, r2)))))
//        GameEvent(position, GameAction.ACTIVATE_YEAR_OF_PLENTY, Some(actionSpec), Some(result))
//      case MonopolyResult(cardsLost) =>
//        val transacted = cardsLost.map({case (p, resources) => (p, ResourceTransaction(lose=getResources(resources)))})
//        // TODO: This should also include the resource in the action spec
//        GameEvent(position, GameAction.ACTIVATE_MONOPOLY, result = Some(ActionResult(resourcesTransacted = transacted)))
//      case PortTradeMove(give, get) =>
//        val transacted = Map((position, ResourceTransaction(gain = getResources(get), lose = getResources(give))))
//        GameEvent(position, GameAction.PORT_TRADE, result = Some(ActionResult(resourcesTransacted = transacted)))
//      case DiscardResourcesMove(resourceSet) =>
//          // BROKEN: This should not be bundled by player, for now just defualting to the first player
//          val first = resourceSet.values.head
//          val transacted = Map((position, ResourceTransaction(lose = getResources(first))))
//          GameEvent(position, GameAction.DISCARD, result = Some(ActionResult(resourcesTransacted = transacted)))
//      case EndTurnMove =>
//        GameEvent(position, GameAction.END_TURN)
//    }
//  }
//
//  private def getResources(resourceSet: CatanResourceSet.Resources): Seq[Resource] = {
//    val resources: mutable.ListBuffer[Resource] = mutable.ListBuffer()
//    resourceSet.amountMap.foreach({ case (res, amt) =>
//        for (i <- 0 to amt) {
//          resources += resourceMap.getOrElse(res.name, null)
//        }
//    })
//    resources.toSeq
//  }
//
//  private def getActionRequestType(request: RequestMessage[GAME]): ActionRequestType = {
//      request match {
//        case InitialPlacementRequest(gameId, playerState, inventory, playerId, first, respondTo) =>
//          ActionRequestType.PLACE_INITIAL_SETTLEMENT
//        case MoveRequest(gameId, playerState, inventory, playerId, respondTo) =>
//          ActionRequestType.BUILD_OR_TRADE_OR_PLAY_OR_PASS
//        case DiscardCardRequest(gameId, playerState, inventory, playerId, respondTo) =>
//          ActionRequestType.DISCARD
//        case MoveRobberRequest(gameId, playerState, inventory, playerId, respondTo) =>
//          // This is just a filler
//          ActionRequestType.ACKNOWLEDGE_PING
//      }
//
//  }

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


object PlayerContext {

  def apply[GAME <: Inventory[GAME]](
    name: String,
    gameId: GameId,
    position: Int,
    observer: StreamObserver[GameMessage]
  ) = new PlayerContext[GAME](name, gameId, position, observer)

}



