package soc.game

import soc.game.board.{Edge, Vertex}
import soc.game.inventory._
import soc.game.inventory.resources.{CatanResourceSet, Steal}
import soc.game.inventory.resources.CatanResourceSet.Resources
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._



sealed trait CatanMove[T]
sealed trait MoveResult[T]

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
  case class RollResult(roll: Roll) extends MoveResult[RollDice]

  case object EndTurnMove extends CatanMove[EndTurn] with MoveResult[EndTurn]

  case class InitialPlacementMove(first: Boolean, settlement: Vertex, road: Edge) extends CatanMove[InitialPlacement] with MoveResult[InitialPlacement]

  case class DiscardResourcesMove(resourceSet: Map[Int, Resources]) extends CatanMove[DiscardResources] with MoveResult[DiscardResources]

  case class MoveRobberAndStealMove(node: Int, playerStole: Option[Int]) extends CatanMove[MoveRobberAndSteal] with ImperfectInformation
  case class MoveRobberAndStealResult(robberLocation: Int, steal: Option[Steal]) extends MoveResult[MoveRobberAndSteal]

  case object BuyDevelopmentCardMove extends CatanBuildMove[BuyDevelopmentCard] with ImperfectInformation
  case class BuyDevelopmentCardResult(card: Option[DevelopmentCard]) extends MoveResult[BuyDevelopmentCard]

  case class BuildRoadMove(edge: Edge) extends CatanBuildMove[BuildRoad] with MoveResult[BuildRoad]
  case class BuildSettlementMove(vertex: Vertex) extends CatanBuildMove[BuildSettlement] with MoveResult[BuildSettlement]
  case class BuildCityMove(vertex: Vertex) extends CatanBuildMove[BuildCity] with MoveResult[BuildCity]

  case class PortTradeMove(give: Resources, get: Resources) extends CatanTradeMove[PortTrade] with MoveResult[PortTrade]

  case class TradeMove(playerId: Int, give: Resources, get: CatanResourceSet[Int]) extends CatanTradeMove[PlayerTrade] with ImperfectInformation
  case object AcceptTrade extends TradeResponse[PlayerTrade]
  case object RejectTrade extends TradeResponse[PlayerTrade]
  case class CounterTrade(playerIdGive: Int, give: Resources, playerIdGet: Int, get: Resources) extends TradeResponse[PlayerTrade]

  case class KnightMove(robber: MoveRobberAndStealMove) extends CatanPlayCardMove[Knight] with ImperfectInformation
  case class KnightResult(robber: MoveRobberAndStealResult) extends MoveResult[Knight]
  case class YearOfPlentyMove(res1: Resource, res2: Resource) extends CatanPlayCardMove[YearOfPlenty] with MoveResult[YearOfPlenty]
  case class MonopolyMove(res: Resource) extends CatanPlayCardMove[Monopoly] with ImperfectInformation
  case class MonopolyResult(cardsLost: Map[Int, Resources]) extends MoveResult[Monopoly]
  case class RoadBuilderMove(road1: Edge, road2: Option[Edge]) extends CatanPlayCardMove[RoadBuilder] with MoveResult[RoadBuilder]
  //case object PointMove extends CatanPlayCardMove[]

}






