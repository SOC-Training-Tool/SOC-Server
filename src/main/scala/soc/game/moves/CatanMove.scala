package soc.game.moves

import soc.game.Roll
import soc.game.board.{Edge, Vertex}
import soc.game.inventory._
import soc.game.inventory.resources.CatanResourceSet.Resources
import soc.game.inventory.resources.{CatanResourceSet, Steal}


sealed trait CatanMove
sealed trait MoveResult

trait CatanBuildMove extends CatanMove
trait CatanTradeMove extends CatanMove
sealed trait CatanPlayCardMove extends CatanMove
trait TradeResponse extends CatanTradeMove

sealed trait ImperfectInformation


  case object RollDiceMove extends CatanMove with ImperfectInformation
  case class RollResult(roll: Roll) extends MoveResult

  case object EndTurnMove extends CatanMove with MoveResult

  case class InitialPlacementMove(first: Boolean, settlement: Vertex, road: Edge) extends CatanMove with MoveResult

  case class DiscardResourcesMove(resourceSet: Map[Int, Resources]) extends CatanMove with MoveResult

  case class MoveRobberAndStealMove(node: Int, playerStole: Option[Int]) extends CatanMove with ImperfectInformation
  case class MoveRobberAndStealResult(robberLocation: Int, steal: Option[Steal]) extends MoveResult

  case object BuyDevelopmentCardMove extends CatanBuildMove with ImperfectInformation
  case class BuyDevelopmentCardResult(card: Option[DevelopmentCard]) extends MoveResult

  case class BuildRoadMove(edge: Edge) extends CatanBuildMove with MoveResult
  case class BuildSettlementMove(vertex: Vertex) extends CatanBuildMove with MoveResult
  case class BuildCityMove(vertex: Vertex) extends CatanBuildMove with MoveResult

  case class PortTradeMove(give: Resources, get: Resources) extends CatanTradeMove with MoveResult

  case class TradeMove(playerId: Int, give: Resources, get: CatanResourceSet[Int]) extends CatanTradeMove with ImperfectInformation
  case object AcceptTrade extends TradeResponse
  case object RejectTrade extends TradeResponse
  case class CounterTrade(playerIdGive: Int, give: Resources, playerIdGet: Int, get: Resources) extends TradeResponse

  case class KnightMove(robber: MoveRobberAndStealMove) extends CatanPlayCardMove with ImperfectInformation
  case class KnightResult(robber: MoveRobberAndStealResult) extends MoveResult
  case class YearOfPlentyMove(res1: Resource, res2: Resource) extends CatanPlayCardMove with MoveResult
  case class MonopolyMove(res: Resource) extends CatanPlayCardMove with ImperfectInformation
  case class MonopolyResult(cardsLost: Map[Int, Resources]) extends MoveResult
  case class RoadBuilderMove(road1: Edge, road2: Option[Edge]) extends CatanPlayCardMove with MoveResult
  //case object PointMove extends CatanPlayCardMove[]