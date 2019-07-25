package soc.game

import soc.game.board.{Edge, Vertex}
import soc.game.resources.{CatanResourceSet, Steal}
import soc.game.resources.CatanResourceSet.Resources
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._



sealed trait CatanMove[T]
sealed trait MoveResult[T] {
  def applyMove(playerId: Int, gameState: GameState): GameState
}

trait CatanBuildMove[T] extends CatanMove[T]
trait CatanTradeMove[T] extends CatanMove[T]
sealed trait CatanPlayCardMove[T] extends CatanMove[T]
trait TradeResponse[T] extends CatanTradeMove[T]

sealed trait ImperfectInformation



object CatanMove {

  type Move = soc.game.CatanMove[_]

  type RollDice
  type EndTurn
  type InitialPlacement
  type DiscardResources
  type MoveRobberAndSteal
  type BuyDevelopmentCard
  type BuildRoad
  type BuildSettlement
  type BuildCity
  type PortTrade
  type Knight
  type Monopoly
  type YearOfPlenty
  type RoadBuilder
  type PlayerTrade

  case object RollDiceMove extends CatanMove[RollDice] with ImperfectInformation
  case class RollResult(roll: Roll) extends MoveResult[RollDice] {
    override def applyMove(playerId: Int, gameState: GameState): GameState = gameState.rollDice(playerId, roll)
  }

  case object EndTurnMove extends CatanMove[EndTurn] with MoveResult[EndTurn]  {
    override def applyMove(playerId: Int, gameState: GameState): GameState = gameState.endTurn(playerId)
  }

  case class InitialPlacementMove(first: Boolean, settlement: Vertex, road: Edge) extends CatanMove[InitialPlacement] with MoveResult[InitialPlacement] {
    override def applyMove(playerId: Int, gameState: GameState): GameState = gameState.initialPlacement(playerId, first, settlement, road)
  }

  case class DiscardResourcesMove(resourceSet: Resources) extends CatanMove[DiscardResources] with MoveResult[DiscardResources] {
    override def applyMove(playerId: Int, gameState: GameState): GameState = gameState.playersDiscardFromSeven(Map(playerId -> resourceSet))
  }

  case class MoveRobberAndStealMove(node: Int, playerStole: Option[Int]) extends CatanMove[MoveRobberAndSteal] with ImperfectInformation
  case class MoveRobberAndStealResult(robberLocation: Int, steal: Option[Steal]) extends MoveResult[MoveRobberAndSteal] {
    override def applyMove(playerId: Int, gameState: GameState): GameState = gameState.moveRobberAndSteal(playerId, robberLocation, steal)
  }

  case object BuyDevelopmentCardMove extends CatanBuildMove[BuyDevelopmentCard] with ImperfectInformation
  case class BuyDevelopmentCardResult(card: Option[DevelopmentCard]) extends MoveResult[BuyDevelopmentCard] {
    override def applyMove(playerId: Int, gameState: GameState): GameState = gameState.buyDevelopmentCard(playerId, card)
  }

  case class BuildRoadMove(edge: Edge) extends CatanBuildMove[BuildRoad] with MoveResult[BuildRoad] {
    override def applyMove(playerId: Int, gameState: GameState): GameState = gameState.buildRoad(playerId, edge)
  }
  case class BuildSettlementMove(vertex: Vertex) extends CatanBuildMove[BuildSettlement] with MoveResult[BuildSettlement] {
    override def applyMove(playerId: Int, gameState: GameState): GameState = gameState.buildSettlement(playerId, vertex)
  }
  case class BuildCityMove(vertex: Vertex) extends CatanBuildMove[BuildCity] with MoveResult[BuildCity] {
    override def applyMove(playerId: Int, gameState: GameState): GameState = gameState.buildCity(playerId, vertex)
  }

  case class PortTradeMove(give: Resources, get: Resources) extends CatanTradeMove[PortTrade] with MoveResult[PortTrade] {
    override def applyMove(playerId: Int, gameState: GameState): GameState = gameState.portTrade(playerId, give, get)
  }

  case class TradeMove(playerId: Int, give: Resources, get: CatanResourceSet[Int]) extends CatanTradeMove[PlayerTrade] with ImperfectInformation
  case object AcceptTrade extends TradeResponse[PlayerTrade]
  case object RejectTrade extends TradeResponse[PlayerTrade]
  case class CounterTrade(playerIdGive: Int, give: Resources, playerIdGet: Int, get: Resources) extends TradeResponse[PlayerTrade]

  case class KnightMove(robber: MoveRobberAndStealMove) extends CatanPlayCardMove[Knight] with ImperfectInformation
  case class KnightResult(robber: MoveRobberAndStealResult) extends MoveResult[Knight] {
    override def applyMove(playerId: Int, gameState: GameState): GameState = gameState.playKnight(playerId, robber.robberLocation, robber.steal)
  }
  case class YearOfPlentyMove(res1: Resource, res2: Resource) extends CatanPlayCardMove[YearOfPlenty] with MoveResult[YearOfPlenty] {
    override def applyMove(playerId: Int, gameState: GameState): GameState = gameState.playYearOfPlenty(playerId, res1, res2)
  }
  case class MonopolyMove(res: Resource) extends CatanPlayCardMove[Monopoly] with ImperfectInformation
  case class MonopolyResult(cardsLost: Map[Int, Resources]) extends MoveResult[Monopoly] {
    override def applyMove(playerId: Int, gameState: GameState): GameState = gameState.playMonopoly(playerId, cardsLost)
  }
  case class RoadBuilderMove(road1: Edge, road2: Option[Edge]) extends CatanPlayCardMove[RoadBuilder] with MoveResult[RoadBuilder] {
    override def applyMove(playerId: Int, gameState: GameState): GameState = gameState.playRoadBuilder(playerId, road1, road2)
  }
  //case object PointMove extends CatanPlayCardMove[]

}






