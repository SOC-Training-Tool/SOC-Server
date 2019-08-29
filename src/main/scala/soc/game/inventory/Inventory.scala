package soc.game.inventory

import soc.game.inventory.Inventory._
import soc.game.inventory.developmentCard.{BuyDevelopmentCard, DevCardInventory, DevCardTransaction, PlayDevelopmentCard}
import soc.game.inventory.developmentCard.DevCardInventory.{DevelopmentCardSet, PlayedInventory}
import soc.game.inventory.resources.CatanResourceSet.Resources
import soc.game.inventory.resources.{CatanResourceSet, Gain, Lose, ProbableResourceSet, SOCTransactions, Steal}

trait Inventory[T <: Inventory[T]] { self: T =>

  type UpdateRes
  type UpdateDev

  val position: Int
  val playedDevCards: PlayedInventory
  val numUnplayedDevCards: Int
  val numCards: Int
  def canBuild(resSet: Resources): Boolean
  def endTurn: T
  def updateResources(update: UpdateRes): T
  def updateDevelopmentCard(update: UpdateDev): T
}

case object Inventory {
  type Inventory = soc.game.inventory.Inventory[_]

  type PerfectInfo = PerfectInfoInventory
  type ProbableInfo = ProbableInfoInventory
  type NoInfo = NoInfoInventory
}


case class PerfectInfoInventory(
  position: Int,
  resourceSet: Resources = CatanResourceSet.empty,
  playedDevCards: PlayedInventory = DevCardInventory(),
  canPlayDevCards: PlayedInventory = DevCardInventory(),
  cannotPlayDevCards: PlayedInventory = DevCardInventory()
) extends Inventory[PerfectInfo] {

  type UpdateRes = List[SOCTransactions]
  type UpdateDev = DevCardTransaction

  override def updateResources(transactions: List[SOCTransactions]): PerfectInfoInventory = {
    val res = transactions.foldLeft(resourceSet){
      case (newSet, Gain(`position`, set)) => newSet.add(set)
      case (newSet, Lose(`position`, set)) => newSet.subtract(set)
      case (newSet, Steal(`position`, _, Some(set))) => newSet.add(set)
      case (newSet, Steal(_, `position`, Some(set))) => newSet.subtract(set)
      case (newSet, _) => newSet
    }
    copy(resourceSet = res)
  }

  override def updateDevelopmentCard(transaction: DevCardTransaction): PerfectInfoInventory = {
    transaction match {
      case BuyDevelopmentCard(`position`, Some(card)) =>
        copy (cannotPlayDevCards = cannotPlayDevCards.add(1, card))
      case PlayDevelopmentCard(`position`, card) =>
        copy (
          canPlayDevCards = canPlayDevCards.subtract(1, card),
          playedDevCards = playedDevCards.add(1, card)
        )
      case _ => copy()
    }

  }

  override def endTurn: PerfectInfoInventory = {
    val newCanPlayDevCards = canPlayDevCards.add(cannotPlayDevCards)
    copy (
      canPlayDevCards = newCanPlayDevCards,
      cannotPlayDevCards = DevCardInventory.empty
    )
  }

  def buyDevelopmentCard(dCard: Option[DevelopmentCard]): PerfectInfoInventory = copy (
    cannotPlayDevCards = dCard.fold(cannotPlayDevCards)(cannotPlayDevCards.add(1, _))
  )

  override val numUnplayedDevCards: Int = cannotPlayDevCards.getTotal + canPlayDevCards.getTotal
  override val numCards: Int = resourceSet.getTotal

  override def canBuild(resSet: Resources): Boolean = resourceSet.contains(resSet)
}

case class NoInfoInventory(
  position: Int,
  playedDevCards: PlayedInventory = DevCardInventory(),
  numCards: Int = 0,
  numUnplayedDevCards: Int = 0) extends Inventory[NoInfo] {

  type UpdateRes = List[SOCTransactions]
  type UpdateDev = DevCardTransaction

  override def updateResources(transactions: List[SOCTransactions]): NoInfoInventory = {
    val numCrds = transactions.foldLeft(numCards) {
      case (num, Gain(`position`, set)) => num + set.getTotal
      case (num, Lose(`position`, set)) => num - set.getTotal
      case (num, Steal(`position`, _, _)) => num + 1
      case (num, Steal(_, `position`, _)) => num - 1
      case (num, _) => num
    }
    copy(numCards = numCrds)
  }

  override def updateDevelopmentCard(transaction: DevCardTransaction): NoInfoInventory = {
    transaction match {
      case BuyDevelopmentCard(`position`, Some(card)) =>
        copy(numUnplayedDevCards = numUnplayedDevCards + 1)
      case PlayDevelopmentCard(`position`, card) =>
        copy(
          numUnplayedDevCards = numUnplayedDevCards - 1,
          playedDevCards = playedDevCards.add(1, card)
        )
      case _ => copy()
    }

  }

  override def endTurn: NoInfoInventory = copy()

  override def canBuild(resSet: Resources): Boolean = true
}

case class ProbableInfoInventory(
  position: Int,
  playedDevCards: PlayedInventory,
  probableResourceSet: ProbableResourceSet,
  knownUnplayedDevCards: PlayedInventory,
  probableDevCards: DevelopmentCardSet[Double]
) extends Inventory[ProbableInfo]  {

  type UpdateRes = ProbableResourceSet
  type UpdateDev = (PlayedInventory, PlayedInventory, DevelopmentCardSet[Double])

  override val numUnplayedDevCards: Int = probableDevCards.getTotal.toInt + knownUnplayedDevCards.getTotal
  override val numCards: Int = probableResourceSet.getTotal

  override def canBuild(resSet: Resources): Boolean = probableResourceSet.mightContain(resSet)

  override def updateResources(probableSet: ProbableResourceSet): ProbableInfoInventory = copy(probableResourceSet = probableSet)

  override def endTurn: ProbableInfoInventory = copy()

  override def updateDevelopmentCard(update: (PlayedInventory, PlayedInventory, DevelopmentCardSet[Double])): ProbableInfoInventory = {
    val (played, known, probable) = update
    copy(
      playedDevCards = played,
      knownUnplayedDevCards = known,
      probableDevCards = probable
    )
  }
}