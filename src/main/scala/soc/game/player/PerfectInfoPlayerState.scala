package soc.game.player

import soc.game.DevCardInventory.{PlayedInventory, UnplayedInventory}
import soc.game._
import soc.game.board.{CatanBoard, Edge, Vertex}
import soc.game.resources.CatanResourceSet.{ResourceSet, Resources}
import soc.game.resources.{CatanResourceSet, Gain, Lose, PossibleHands, SOCTransactions, Steal}

import scala.util.Random

case class PerfectInfoPlayerState(
  name: String,
  position: Int,
  resourceSet: Resources = CatanResourceSet.empty,
  armyPoints: Int = 0,
  roadPoints: Int = 0,
  dCardPoints: Int = 0,
  ports: Set[Port] = Set.empty,
  settlements: List[Vertex] = Nil,
  cities: List[Vertex] = Nil,
  roads: List[Edge] = Nil,
  dots: ResourceSet[Int] = CatanResourceSet.empty,
  roadLength: Int = 0,
  playedDevCards: PlayedInventory = DevCardInventory(),
  canPlayDevCards: PlayedInventory = DevCardInventory(),
  cannotPlayDevCards: PlayedInventory = DevCardInventory()
) extends PlayerState {

  override val numCards: Int = resourceSet.getTotal

  override def getRandomCard(implicit random: Random): Option[Resource] = {
    random.shuffle(resourceSet.amountMap.flatMap {
      case (resource, amount) => (1 to amount).map(_ => resource)
    }).headOption
  }

  override def getAmount(resource: Resource): Double = resourceSet.getAmount(resource).toDouble

  override val canBuildRoad: Boolean = super.canBuildRoad && resourceSet.contains(Road.cost)

  override val canBuildSettlement: Boolean = super.canBuildSettlement && resourceSet.contains(Settlement.cost)

  override val canBuildCity: Boolean = super.canBuildCity && resourceSet.contains(City.cost)

  override val canBuyDevCard: Boolean = super.canBuyDevCard && resourceSet.contains(DevelopmentCard.cost)

  override def updateResources(transactions: List[SOCTransactions]): PlayerState = {
    val res = transactions.foldLeft(resourceSet){
      case (newSet, Gain(`position`, set)) => newSet.add(set)
      case (newSet, Lose(`position`, set)) => newSet.subtract(set)
      case (newSet, Steal(`position`, _, Some(set))) => newSet.add(set)
      case (newSet, Steal(_, `position`, Some(set))) => newSet.subtract(set)
      case (newSet, _) => newSet
    }
    copy(resourceSet = res)
  }

  override val numUnplayeDevCards: Int = canPlayDevCards.getTotal + cannotPlayDevCards.getTotal

  override def playDevelopmentCard(card: DevelopmentCard): PlayerState = copy (
    dCardPoints = card match {
      case CatanPoint => dCardPoints + 1
      case _ => dCardPoints
    },
    canPlayDevCards = canPlayDevCards.subtract(1, card),
    playedDevCards = playedDevCards.add(1, card)
  )

  override def endTurn: PlayerState = {
    val newCanPlayDevCards = canPlayDevCards.add(cannotPlayDevCards)
    copy (
      canPlayDevCards = newCanPlayDevCards,
      cannotPlayDevCards = DevCardInventory()
    )
  }

  override def buyDevelopmentCard(dCard: Option[DevelopmentCard]): PlayerState = copy (
    cannotPlayDevCards = dCard.fold(cannotPlayDevCards)(cannotPlayDevCards.add(1, _))
  )

  override def _copy(name: String, position: Int, armyPoints: Int, roadPoints: Int, dCardPoints: Int, ports: Set[Port], settlements: List[Vertex], cities: List[Vertex], roads: List[Edge], dots: ResourceSet[Int], roadLength: Int, playedDevCards: PlayedInventory): PlayerState = copy(
      name = name,
      position = position,
      armyPoints = armyPoints,
      roadPoints = roadPoints,
      dCardPoints = dCardPoints,
      ports = ports,
      settlements = settlements,
      cities = cities,
      roads = roads,
      dots = dots,
      roadLength = roadLength,
      playedDevCards = playedDevCards
  )


}
