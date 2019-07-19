//package soc.actors
//
//import akka.actor.{Actor, Props}
//import log.Log
//import soc.actors.GameActor._
//import soc.game._
//import soc.game.board.{Edge, Vertex}
//import soc.game.player.moveSelector.MoveSelector
//import soc.game.resources.CatanResourceSet.Resources
//import soc.game.resources.Steal
//
//class PlayerActor(
//  name: String,
//  playerId: Int,
//  moveSelector: MoveSelector) extends Actor {
//
//  val log = new Log {override def print(message: String): Unit = ()}
//  var gameState: GameState = _
//  var ourTurn = false
//
//  override def receive: Receive = {
//
//    case StartingGame(board, players) =>
//      val gamePlayers = players.map {
//        case (`playerId`, `name`) => PerfectInfoPlayerState(name, playerId)
//        case (p, n) => NoInfoPlayerState(n, p)
//      }.toList.sortBy(_.position)
//      gameState = GameState(board, gamePlayers, log)
//
//    case InitialPlacementRequest(`playerId`, first) =>
//      sendResponse(moveSelector.initialPlacementMove(gameState, playerId)(first))
//    case InitialPlacementRequest(_, _) =>
//
//    case DiscardCardRequest(toDiscard) if toDiscard.contains(playerId) =>
//      sendResponse(moveSelector.discardCardsMove(gameState, playerId))
//    case MoveRobberRequest(`playerId`) =>
//      sendResponse(moveSelector.moveRobberAndStealMove(gameState, playerId))
//
//
//
//    case TurnUpdate(`playerId`, _) =>
//      ourTurn = true
//      sendResponse(moveSelector.turnMove(gameState, playerId))
//
//    case DiscardCardRequest(_) =>
//    case TurnUpdate(_, _) =>
//
//    case EndTurnUpdate(position) =>
//      ourTurn = false
//      gameState = gameState.endTurn(position)
//    case InitialPlacementUpdate(position, first, settlement, road) =>
//      gameState = gameState.initialPlacement(position, first, settlement, road)
//    case RollDiceUpdate(position, roll, _) =>
//      gameState = gameState.rollDice(position, roll)
//      if (ourTurn) sendResponse(moveSelector.turnMove(gameState, playerId))
//    case DiscardCardsUpdate(toDiscard) =>
//      gameState = gameState.playersDiscardFromSeven(toDiscard)
//    case MoveRobberUpdate(position, robberLocation, steal) =>
//      gameState = gameState.moveRobberAndSteal(position, robberLocation, steal)
//      if (ourTurn) sendResponse(moveSelector.turnMove(gameState, playerId))
//    case BuildSettlementUpdate(pos, settlement) =>
//      gameState = gameState.buildSettlement(pos, settlement)
//      if (ourTurn) sendResponse(moveSelector.turnMove(gameState, playerId))
//    case BuildCityUpdate(pos, city) =>
//      gameState = gameState.buildCity(pos, city)
//      if (ourTurn) sendResponse(moveSelector.turnMove(gameState, playerId))
//    case BuildRoadUpdate(pos, road) =>
//      gameState = gameState.buildRoad(pos, road)
//      if (ourTurn) sendResponse(moveSelector.turnMove(gameState, playerId))
//    case BuyDevelopmentCardUpdate(position, card) =>
//      gameState = gameState.buyDevelopmentCard(position, card)
//      if (ourTurn) sendResponse(moveSelector.turnMove(gameState, position))
//    case KnightUpdate(position, robberLocation, steal) =>
//      gameState = gameState.playKnight(position, robberLocation, steal)
//      if (ourTurn) sendResponse(moveSelector.turnMove(gameState, position))
//    case YearOfPlentyUpdate(position, res1, res2) =>
//      gameState = gameState.playYearOfPlenty(position, res1, res2)
//      if (ourTurn) sendResponse(moveSelector.turnMove(gameState, playerId))
//    case MonopolyUpdate(playerId, cardsLost) =>
//      gameState = gameState.playMonopoly(playerId, cardsLost)
//      if (ourTurn) sendResponse(moveSelector.turnMove(gameState, playerId))
//    case RoadBuilderUpdate(position, road1, road2) =>
//      gameState = gameState.playRoadBuilder(position, road1, road2)
//      if (ourTurn) sendResponse(moveSelector.turnMove(gameState, position))
//    case PortTradeUpdate(position, portTrade) =>
//      gameState = gameState.portTrade(position, portTrade)
//      if (ourTurn) sendResponse(moveSelector.turnMove(gameState, position))
//  }
//
//  def sendResponse(move: CatanMove): Unit = {
//    import soc.actors.PlayerActor._
//
//    val response = move match {
//      case RollDiceMove => RollDiceResponse
//      case EndTurnMove => EndTurnResponse
//      case InitialPlacementMove(first, settlement, edge) => InitialPlacementResponse(first, settlement, edge)
//      case DiscardResourcesMove(set) => DiscardCardsResponse(set)
//      case MoveRobberAndStealMove(robberLocation, playerStole) => MoveRobberResponse(robberLocation, playerStole)
//      case BuyDevelopmentCardMove => BuyDevelopmentCardResponse
//      case BuildRoad(edge) => BuildRoadResponse(edge)
//      case BuildSettlement(vertex) => BuildSettlementResponse(vertex)
//      case BuildCity(vertex) => BuildCityResponse(vertex)
//      case PortTrade(give, get) => PortTradeResponse(give, get)
//      case card: CatanPlayCardMove => PlayDevelopmentCardResponse(card)
//    }
//    sender ! Response(playerId, response)
//  }
//
//
//}
//
//object PlayerActor {
//
//  def props(name: String, position: Int, moveSelector: MoveSelector): Props = Props(new PlayerActor(name, position, moveSelector))
//
//  case class Response(playerId: Int, request: ResponseMessage)
//  sealed trait ResponseMessage
//
//  case class InitialPlacementResponse(first: Boolean, settlement: Vertex, road: Edge) extends ResponseMessage
//  case object RollDiceResponse extends ResponseMessage
//  case class DiscardCardsResponse(cards: Resources) extends ResponseMessage
//  case class MoveRobberResponse(robberLocation: Int, steal: Option[Int]) extends ResponseMessage
//  case class BuildSettlementResponse(settlement: Vertex) extends ResponseMessage
//  case class BuildCityResponse(city: Vertex) extends ResponseMessage
//  case class BuildRoadResponse(road: Edge) extends ResponseMessage
//  case object BuyDevelopmentCardResponse extends ResponseMessage
//  case class PlayDevelopmentCardResponse(dCard: CatanPlayCardMove) extends ResponseMessage
//  case class PortTradeResponse(give: Resources, get: Resources) extends ResponseMessage
//  case object EndTurnResponse extends ResponseMessage
//
//}
