package soc.akka.messages

import akka.Done
import akka.actor.typed.ActorRef
import soc.game.{GameState, MoveResult, Roll}
import soc.game.board.{BoardConfiguration, CatanBoard, Edge, Vertex}
import soc.game.inventory.{DevelopmentCard, Inventory, Resource}
import soc.game.inventory.resources.CatanResourceSet.Resources
import soc.game.inventory.resources.Steal
import soc.storage.GameId

sealed trait UpdateMessage extends GameMessage {
  val gameId: GameId
}
sealed trait DevelopmentCardUpdate extends UpdateMessage

object UpdateMessage {
  case class StartGame[PLAYER <: Inventory[PLAYER]](gameId: GameId, state: GameState[PLAYER]) extends UpdateMessage
  case class GameOver(gameId: GameId, msg: String) extends UpdateMessage
  case class TurnUpdate(gameId: GameId, playerId: Int, turnNumber: Int = 0) extends UpdateMessage

  case class MoveResultUpdate(gameId: GameId, moveResult: MoveResult) extends UpdateMessage

  case class LongestRoadUpdate(gameId: GameId, playerId: Int) extends UpdateMessage
  case class LargestArmyUpdate(gameId: GameId, playerId: Int) extends UpdateMessage



//  case class InitialPlacementUpdate(gameId: GameId, playerId: Int, first: Boolean, settlement: Vertex, road: Edge) extends UpdateMessage
//  case class TurnUpdate(gameId: GameId, playerId: Int, turnNumber: Int = 0) extends UpdateMessage
//  case class EndTurnUpdate(gameId: GameId, playerId: Int) extends UpdateMessage
//  case class RollDiceUpdate(gameId: GameId, playerId: Int, roll: Roll, resourceGained: Map[Int, Resources]) extends UpdateMessage
//  case class DiscardCardsUpdate(gameId: GameId, cardsDiscarded: Map[Int, Resources]) extends UpdateMessage
//  case class MoveRobberUpdate(gameId: GameId, playerId: Int, robberLocation: Int, steal: Option[Steal]) extends UpdateMessage
//  case class BuildSettlementUpdate(gameId: GameId, playerId: Int, settlement: Vertex) extends UpdateMessage
//  case class BuildCityUpdate(gameId: GameId, playerId: Int, city: Vertex) extends UpdateMessage
//  case class BuildRoadUpdate(gameId: GameId, playerId: Int, road: Edge) extends UpdateMessage
//  case class BuyDevelopmentCardUpdate(gameId: GameId, playerId: Int, card: Option[DevelopmentCard]) extends UpdateMessage
//  case class PortTradeUpdate(gameId: GameId, playerId: Int, give: Resources, get: Resources) extends UpdateMessage
//  //case class TradeUpdate(playerId: Int, trade: TradeMove) extends UpdateMessage
//
//  case class KnightUpdate(gameId: GameId, playerId: Int, robberLocation: Int, steal: Option[Steal]) extends DevelopmentCardUpdate
//  case class PointUpdate(gameId: GameId, playerId: Int) extends DevelopmentCardUpdate
//  case class MonopolyUpdate(gameId: GameId, playerId: Int, resourcesMoved: Map[Int, Resources]) extends DevelopmentCardUpdate
//  case class YearOfPlentyUpdate(gameId: GameId, playerId: Int, res1: Resource, res2: Resource) extends DevelopmentCardUpdate
//  case class RoadBuilderUpdate(gameId: GameId, playerId: Int, road1: Edge, road2: Option[Edge]) extends DevelopmentCardUpdate
}





