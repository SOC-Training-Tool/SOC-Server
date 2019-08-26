package soc.akka.messages

import soc.game.{GameState, Roll}
import soc.game.board.{BoardConfiguration, CatanBoard, Edge, Vertex}
import soc.game.inventory.{DevelopmentCard, Resource}
import soc.game.inventory.resources.CatanResourceSet.Resources
import soc.game.inventory.resources.Steal

sealed trait UpdateMessage extends GameMessage {
  val gameId: Int
}
sealed trait DevelopmentCardUpdate extends UpdateMessage

object UpdateMessage {
  case class StartGame(gameId: Int, board: CatanBoard, players: Seq[(String, Int)]) extends UpdateMessage

  case class InitialPlacementUpdate(gameId: Int, playerId: Int, first: Boolean, settlement: Vertex, road: Edge) extends UpdateMessage
  case class TurnUpdate(gameId: Int, playerId: Int, turnNumber: Int = 0) extends UpdateMessage
  case class EndTurnUpdate(gameId: Int, playerId: Int) extends UpdateMessage
  case class RollDiceUpdate(gameId: Int, playerId: Int, roll: Roll, resourceGained: Map[Int, Resources]) extends UpdateMessage
  case class DiscardCardsUpdate(gameId: Int, cardsDiscarded: Map[Int, Resources]) extends UpdateMessage
  case class MoveRobberUpdate(gameId: Int, playerId: Int, robberLocation: Int, steal: Option[Steal]) extends UpdateMessage
  case class BuildSettlementUpdate(gameId: Int, playerId: Int, settlement: Vertex) extends UpdateMessage
  case class BuildCityUpdate(gameId: Int, playerId: Int, city: Vertex) extends UpdateMessage
  case class BuildRoadUpdate(gameId: Int, playerId: Int, road: Edge) extends UpdateMessage
  case class BuyDevelopmentCardUpdate(gameId: Int, playerId: Int, card: Option[DevelopmentCard]) extends UpdateMessage
  case class LongestRoadUpdate(gameId: Int, playerId: Int) extends UpdateMessage
  case class LargestArmyUpdate(gameId: Int, playerId: Int) extends UpdateMessage
  case class PortTradeUpdate(gameId: Int, playerId: Int, give: Resources, get: Resources) extends UpdateMessage
  //case class TradeUpdate(playerId: Int, trade: TradeMove) extends UpdateMessage

  case class KnightUpdate(gameId: Int, playerId: Int, robberLocation: Int, steal: Option[Steal]) extends DevelopmentCardUpdate
  case class PointUpdate(gameId: Int, playerId: Int) extends DevelopmentCardUpdate
  case class MonopolyUpdate(gameId: Int, playerId: Int, resourcesMoved: Map[Int, Resources]) extends DevelopmentCardUpdate
  case class YearOfPlentyUpdate(gameId: Int, playerId: Int, res1: Resource, res2: Resource) extends DevelopmentCardUpdate
  case class RoadBuilderUpdate(gameId: Int, playerId: Int, road1: Edge, road2: Option[Edge]) extends DevelopmentCardUpdate
}





