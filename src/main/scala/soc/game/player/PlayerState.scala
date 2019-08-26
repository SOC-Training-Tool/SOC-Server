package soc.game.player

import soc.game.GameRules
import soc.game.board.{CatanBoard, Edge, Vertex}
import soc.game.inventory.Inventory
import soc.game.inventory._
import soc.game.inventory.developmentCard.DevCardInventory.PlayedInventory
import soc.game.inventory.developmentCard.PlayDevelopmentCard
import soc.game.inventory.resources.CatanResourceSet.ResourceSet
import soc.game.inventory.resources.{CatanResourceSet, SOCTransactions}

case class PlayerState[T <: Inventory[T]](
  name: String,
  position: Int,
  inventory: T,
  armyPoints: Int = 0,
  roadPoints: Int = 0,
  ports: Set[Port] = Set.empty,
  settlements: List[Vertex] = Nil,
  cities: List[Vertex] = Nil,
  roads: List[Edge] = Nil,
  dots: ResourceSet[Int] = CatanResourceSet.empty,
  roadLength: Int = 0)
  (implicit val gameRules: GameRules) {

  val settlementPoints = settlements.length
  val cityPoints = 2 * cities.length
  val boardPoints: Int = settlementPoints + cityPoints
  val dCardPoints = inventory.playedDevCards.getAmount(CatanPoint)
  val points = boardPoints + armyPoints + roadPoints + dCardPoints

  def canBuildSettlement = settlements.length < gameRules.numSettlements

  def buildSettlement(board: CatanBoard, vertex: Vertex): PlayerState[T] = copy(
    settlements = this.settlements ::: List(vertex),
    ports = board.getPort(vertex).fold(this.ports)(this.ports + _),
    dots = board.adjacentHexes(vertex).flatMap { node =>
      node.hex.getResourceAndNumber.map {
        case (resource, roll) => roll.dots -> resource
      }
    }.foldLeft(this.dots) { case (set, (amt, res)) => set.add(amt, res) }
  )

  def canBuildCity = cities.length < gameRules.numCities

  def buildCity(board: CatanBoard, vertex: Vertex): PlayerState[T] = copy(
    settlements = this.settlements.filterNot(_ == vertex),
    cities = this.cities ::: List(vertex),
    dots = board.adjacentHexes(vertex).flatMap { node =>
      node.hex.getResourceAndNumber.map {
        case (resource, roll) => roll.dots -> resource
      }
    }.foldLeft(this.dots) { case (set, (amt, res)) => set.add(amt, res) }
  )

  def canBuildRoad = roads.length < gameRules.numRoads

  def buildRoad(board: CatanBoard, edge: Edge): PlayerState[T] = copy(
    roads = this.roads ::: List(edge),
    roadLength = board.buildRoad(edge, position).roadLengths.get(position).getOrElse(0)
  )

  val playedDevCards: PlayedInventory = inventory.playedDevCards

  val numUnplayedDevCards: Int = inventory.numUnplayedDevCards

  val numCards: Int = inventory.numCards

  val hasLongestRoad = roadPoints > 0

  val hasLargestArmy = armyPoints > 0

  def gainLongestRoad = copy(roadPoints = 2)

  def loseLongestRoad = copy(roadPoints = 0)

  def gainLargestArmy = copy(armyPoints = 2)

  def loseLargestArmy = copy(armyPoints = 0)

  def updateResources(transactions: inventory.UpdateRes): PlayerState[T] = {
    copy(
      inventory = inventory.updateResources(transactions)
    )
  }

  def updateDevelopmentCard(card: inventory.UpdateDev): PlayerState[T]= copy (
    inventory = inventory.updateDevelopmentCard(card)
  )

  def canBuyDevelopmentCard = true

  def endTurn: PlayerState[T] = copy(inventory = inventory.endTurn)

}


//  def getStateArray: List[Double] = {
//    position.toDouble ::
//      numCards.toDouble ::
//      dots.getAmount(Brick).toDouble ::
//      dots.getAmount(Wood).toDouble ::
//      dots.getAmount(Sheep).toDouble ::
//      dots.getAmount(Wheat).toDouble ::
//      dots.getAmount(Ore).toDouble ::
//      points.toDouble ::
//      boardPoints.toDouble ::
//      armyPoints.toDouble ::
//      roadPoints.toDouble ::
//      dCardPoints.toDouble ::
//      playedDevCards.getTotal.toDouble ::
//      playedDevCards.getAmount(Knight).toDouble ::
//      playedDevCards.getAmount(Monopoly).toDouble ::
//      playedDevCards.getAmount(RoadBuilder).toDouble ::
//      playedDevCards.getAmount(YearOfPlenty).toDouble ::
//      playedDevCards.getAmount(CatanPoint).toDouble ::
//      numUnplayeDevCards.toDouble ::
//      (if (ports.contains(Brick)) 1.0 else 0.0) ::
//      (if (ports.contains(Ore)) 1.0 else 0.0) ::
//      (if (ports.contains(Sheep)) 1.0 else 0.0) ::
//      (if (ports.contains(Wheat)) 1.0 else 0.0) ::
//      (if (ports.contains(Wood)) 1.0 else 0.0) ::
//      (if (ports.contains(Misc)) 1.0 else 0.0) ::
//      (settlements.map(_.node.toDouble) ::: (1 to (5 - settlements.size)).map(_ => 0.0).toList) :::
//      (cities.map(_.node.toDouble) ::: (1 to (4 - cities.size)).map(_ => 0.0).toList) //:::
//    // (roads.map(_.toDouble) ::: (1 to (15 - roads.size)).map(_ => 0.0).toList)
//  }

