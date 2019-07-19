package soc.game.messages

import soc.game.{DevelopmentCard, Resource, Roll}
import soc.game.board.{Edge, Vertex}
import soc.game.resources.CatanResourceSet.Resources
import soc.game.resources.Steal

sealed trait UpdateMessage extends GameMessage
sealed trait DevelopmentCardUpdate extends UpdateMessage

object UpdateMessage {
  case class InitialPlacementUpdate(playerId: Int, first: Boolean, settlement: Vertex, road: Edge) extends UpdateMessage
  case class TurnUpdate(playerId: Int, turnNumber: Int = 0) extends UpdateMessage
  case class EndTurnUpdate(playerId: Int) extends UpdateMessage
  case class RollDiceUpdate(playerId: Int, roll: Roll, resourceGained: Map[Int, Resources]) extends UpdateMessage
  case class DiscardCardsUpdate(cardsDiscarded: Map[Int, Resources]) extends UpdateMessage
  case class MoveRobberUpdate(playerId: Int, robberLocation: Int, steal: Option[Steal]) extends UpdateMessage
  case class BuildSettlementUpdate(playerId: Int, settlement: Vertex) extends UpdateMessage
  case class BuildCityUpdate(playerId: Int, city: Vertex) extends UpdateMessage
  case class BuildRoadUpdate(playerId: Int, road: Edge) extends UpdateMessage
  case class BuyDevelopmentCardUpdate(playerId: Int, card: Option[DevelopmentCard]) extends UpdateMessage
  case class LongestRoadUpdate(playerId: Int) extends UpdateMessage
  case class LargestArmyUpdate(playerId: Int) extends UpdateMessage
  case class PortTradeUpdate(playerId: Int, give: Resources, get: Resources) extends UpdateMessage
  //case class TradeUpdate(playerId: Int, trade: TradeMove) extends UpdateMessage

  case class KnightUpdate(playerId: Int, robberLocation: Int, steal: Option[Steal]) extends DevelopmentCardUpdate
  case class PointUpdate(playerId: Int) extends DevelopmentCardUpdate
  case class MonopolyUpdate(playerId: Int, resourcesMoved: Map[Int, Resources]) extends DevelopmentCardUpdate
  case class YearOfPlentyUpdate(playerId: Int, res1: Resource, res2: Resource) extends DevelopmentCardUpdate
  case class RoadBuilderUpdate(playerId: Int, road1: Edge, road2: Option[Edge]) extends DevelopmentCardUpdate

}





