package soc.game.player

import soc.game.DevCardInventory.{PlayedInventory, UnplayedInventory}
import soc.game.{DevCardInventory, DevelopmentCard, Port, Resource, Wood}
import soc.game.board.{Edge, Vertex}
import soc.game.resources.{CatanResourceSet, Gain, Lose, PossibleHands, ProbableResourceSet, SOCTransactions, Steal}
import soc.game.resources.CatanResourceSet.{ResourceSet, Resources}

import scala.util.Random

case class NoInfoPlayerState(
  name: String,
  position: Int,
  numCards: Int = 0,
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
  numUnplayeDevCards: Int = 0
) extends PlayerState {

  override def getAmount(resource: Resource): Double = 0

  override def updateResources(transactions: List[SOCTransactions]): PlayerState = {
    val numCrds = transactions.foldLeft(numCards){
      case (num, Gain(`position`, set)) => num + set.getTotal
      case (num, Lose(`position`, set)) => num - set.getTotal
      case (num, Steal(`position`, _, _)) => num + 1
      case (num, Steal(_, `position`, _)) =>num - 1
      case (num, _) => num
    }
    copy(numCards = numCrds)
  }

  override def getRandomCard(implicit random: Random): Option[Resource] = None

  override def playDevelopmentCard(card: DevelopmentCard): PlayerState = copy (
    numUnplayeDevCards = numUnplayeDevCards - 1,
    playedDevCards = playedDevCards.add(1, card)
  )

  override def buyDevelopmentCard(card: Option[DevelopmentCard]): PlayerState = copy (
    numUnplayeDevCards = numUnplayeDevCards + 1
  )

  override def endTurn: PlayerState = copy()

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