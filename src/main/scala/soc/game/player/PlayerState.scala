package soc.game.player

import soc.game.DevCardInventory.{PlayedInventory, UnplayedInventory}
import soc.game.board.{CatanBoard, Edge, Vertex}
import soc.game.resources.{CatanResourceSet, PossibleHands, ProbableResourceSet, SOCTransactions}
import soc.game._
import soc.game.resources.CatanResourceSet.ResourceSet

import scala.util.Random

trait PlayerState {

  val name: String
  val position: Int

  val settlementPoints = settlements.length
  val cityPoints = 2 * cities.length
  val boardPoints: Int = settlementPoints + cityPoints

  val armyPoints: Int
  val roadPoints: Int
  val dCardPoints: Int

  val points = boardPoints + armyPoints + roadPoints + dCardPoints

  val ports: Set[Port]
  val settlements: List[Vertex]
  val cities: List[Vertex]
  val roads: List[Edge]
  val dots: ResourceSet[Int]

  val roadLength: Int

  val playedDevCards: PlayedInventory

  val numUnplayeDevCards: Int

  val numCards: Int
  def getAmount(resource: Resource): Double

  def updateResources(transactions: List[SOCTransactions]): PlayerState

  def canBuildSettlement: Boolean = settlements.length < 5

  def canBuildCity: Boolean = cities.length < 4

  def canBuildRoad: Boolean = roads.length < 15

  def canBuyDevCard: Boolean = true

  def buildSettlement(board: CatanBoard, vertex: Vertex): PlayerState = _copy(
    settlements = this.settlements ::: List(vertex),
    ports = board.getPort(vertex).fold(this.ports)(this.ports + _),
    dots = board.adjacentHexes(vertex).flatMap { node =>
      node.hex.getResourceAndNumber.map {
        case (resource, roll) => roll.dots -> resource
      }
    }.foldLeft(this.dots) { case (set, (amt, res)) => set.add(amt, res) }
  )

  def getRandomCard(implicit random: Random): Option[Resource]

  def buildCity(board: CatanBoard, vertex: Vertex): PlayerState = _copy (
    settlements = this.settlements.filterNot(_ == vertex),
    cities = this.cities ::: List(vertex),
    dots = board.adjacentHexes(vertex).flatMap { node =>
      node.hex.getResourceAndNumber.map {
        case (resource, roll) => roll.dots -> resource
      }
    }.foldLeft(this.dots) { case (set, (amt, res)) => set.add(amt, res) }
  )

  def buildRoad(board: CatanBoard, edge: Edge): PlayerState  = _copy (
    roads = this.roads ::: List(edge),
    roadLength = board.buildRoad(edge, position).roadLengths.get(position).getOrElse(0)
  )

  def gainLongestRoad = _copy(roadPoints = 2)
  def loseLongestRoad = _copy(roadPoints = 0)
  def gainLargestArmy = _copy(armyPoints = 2)
  def loseLargestArmy = _copy(armyPoints = 0)

  def playDevelopmentCard(card: DevelopmentCard): PlayerState
  def buyDevelopmentCard(card: Option[DevelopmentCard]): PlayerState

  def endTurn: PlayerState

  def _copy(
    name: String = name,
    position: Int = position,
    armyPoints: Int = armyPoints,
    roadPoints: Int = roadPoints,
    dCardPoints: Int = dCardPoints,
    ports: Set[Port] = ports,
    settlements: List[Vertex] = settlements,
    cities: List[Vertex] = cities,
    roads: List[Edge] = roads,
    dots: ResourceSet[Int] = dots,
    roadLength: Int = roadLength,
    playedDevCards: PlayedInventory = playedDevCards
  ): PlayerState

  def getStateArray: List[Double] = {
    position.toDouble ::
      numCards.toDouble ::
      getAmount(Brick) ::
      getAmount(Wood) ::
      getAmount(Sheep) ::
      getAmount(Wheat) ::
      getAmount(Ore) ::
      dots.getAmount(Brick).toDouble ::
      dots.getAmount(Wood).toDouble ::
      dots.getAmount(Sheep).toDouble ::
      dots.getAmount(Wheat).toDouble ::
      dots.getAmount(Ore).toDouble ::
      points.toDouble ::
      boardPoints.toDouble ::
      armyPoints.toDouble ::
      roadPoints.toDouble ::
      dCardPoints.toDouble ::
      playedDevCards.getTotal.toDouble ::
      playedDevCards.getAmount(Knight).toDouble ::
      playedDevCards.getAmount(Monopoly).toDouble ::
      playedDevCards.getAmount(RoadBuilder).toDouble ::
      playedDevCards.getAmount(YearOfPlenty).toDouble ::
      playedDevCards.getAmount(CatanPoint).toDouble ::
      numUnplayeDevCards.toDouble ::
      (if (ports.contains(Brick)) 1.0 else 0.0) ::
      (if (ports.contains(Ore)) 1.0 else 0.0) ::
      (if (ports.contains(Sheep)) 1.0 else 0.0) ::
      (if (ports.contains(Wheat)) 1.0 else 0.0) ::
      (if (ports.contains(Wood)) 1.0 else 0.0) ::
      (if (ports.contains(Misc)) 1.0 else 0.0) ::
      (settlements.map(_.node.toDouble) ::: (1 to (5 - settlements.size)).map(_ => 0.0).toList) :::
      (cities.map(_.node.toDouble) ::: (1 to (4 - cities.size)).map(_ => 0.0).toList) //:::
    // (roads.map(_.toDouble) ::: (1 to (15 - roads.size)).map(_ => 0.0).toList)
  }
}
