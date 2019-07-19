package soc.game

import soc.game.board.{Edge, Vertex}
import soc.game.resources.CatanResourceSet
import soc.game.resources.CatanResourceSet.Resources

sealed trait CatanMove
trait CatanBuildMove extends CatanMove
trait CatanTradeMove extends CatanMove
sealed trait CatanPlayCardMove extends CatanMove
trait TradeResponse extends CatanTradeMove

sealed trait ImperfectInformation

object CatanMove {
  case object RollDiceMove extends CatanMove with ImperfectInformation
  case object EndTurnMove extends CatanMove

  case class InitialPlacementMove(first: Boolean, settlement: Vertex, road: Edge) extends CatanMove

  case class DiscardResourcesMove(resourceSet: Resources) extends CatanMove
  case class MoveRobberAndStealMove(node: Int, playerStole: Option[Int]) extends CatanMove with ImperfectInformation

  case object BuyDevelopmentCardMove extends CatanBuildMove with ImperfectInformation

  case class BuildRoad(edge: Edge) extends CatanBuildMove
  case class BuildSettlement(vertex: Vertex) extends CatanBuildMove
  case class BuildCity(vertex: Vertex) extends CatanBuildMove

  case class PortTrade(give: Resources, get: Resources) extends CatanTradeMove

  case class TradeMove(playerId: Int, give: Resources, get: CatanResourceSet[Int]) extends CatanTradeMove with ImperfectInformation
  case object AcceptTrade extends TradeResponse
  case object RejectTrade extends TradeResponse
  case class CounterTrade(playerIdGive: Int, give: Resources, playerIdGet: Int, get: Resources) extends TradeResponse

  case class KnightMove(robber: MoveRobberAndStealMove) extends CatanPlayCardMove with ImperfectInformation
  case class YearOfPlentyMove(res1: Resource, res2: Resource) extends CatanPlayCardMove
  case class MonopolyMove(res: Resource) extends CatanPlayCardMove with ImperfectInformation
  case class RoadBuilderMove(road1: Edge, road2: Option[Edge]) extends CatanPlayCardMove
  case object PointMove extends CatanPlayCardMove
}

