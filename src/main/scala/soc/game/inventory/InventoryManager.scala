package soc.game.inventory

import soc.game.GameRules
import soc.game.inventory.Inventory._
import soc.game.inventory.developmentCard.{BuyDevelopmentCard, PlayDevelopmentCard, PossibleDevelopmentCards}
import soc.game.inventory.resources.{PossibleHands, SOCPossibleHands, SOCTransactions}
import soc.game.player.PlayerState

trait InventoryManager[T <: Inventory[T]] {

  implicit val gameRules: GameRules

  def updateResources(players: Map[Int, PlayerState[T]], transactions: List[SOCTransactions]): (Map[Int, PlayerState[T]], InventoryManager[T])
  def playDevelopmentCard(players: Map[Int, PlayerState[T]], id: Int, card: DevelopmentCard): (Map[Int, PlayerState[T]], InventoryManager[T])
  def buyDevelopmentCard(players: Map[Int, PlayerState[T]], id: Int, card: Option[DevelopmentCard]): (Map[Int, PlayerState[T]], InventoryManager[T])
  def createInventory(position: Int): T
}

case class PerfectInfoInventoryManager(implicit val gameRules: GameRules) extends InventoryManager[PerfectInfo] {

  override def updateResources(players: Map[Int, PlayerState[PerfectInfo]], transactions: List[SOCTransactions]): (Map[Int, PlayerState[PerfectInfo]], InventoryManager[PerfectInfo]) = {
    (players.mapValues(_.updateResources(transactions)), this)
  }

  override def playDevelopmentCard(players: Map[Int, PlayerState[PerfectInfo]], id: Int, card: DevelopmentCard): (Map[Int, PlayerState[PerfectInfo]], InventoryManager[PerfectInfo]) = {
    (players.map {
        case(`id`, ps) => id -> ps.updateDevelopmentCard(PlayDevelopmentCard(id, card))
        case (i, ps) => i -> ps
      }, this)
  }

  override def buyDevelopmentCard(players: Map[Int, PlayerState[PerfectInfo]], id: Int, card: Option[DevelopmentCard]): (Map[Int, PlayerState[PerfectInfo]], InventoryManager[PerfectInfo]) = {
    (players.map {
      case(`id`, ps) => id -> ps.updateDevelopmentCard(BuyDevelopmentCard(id, card))
      case (i, ps) => i -> ps
    }, this)
  }

  override def createInventory(position: Int): PerfectInfo = new PerfectInfoInventory(position)
}

case class ProbableInfoInventoryManager(
  possibleHands: PossibleHands,
  possibleDevCards: PossibleDevelopmentCards)
  (implicit val gameRules: GameRules) extends InventoryManager[ProbableInfo] {

  override def updateResources(players: Map[Int, PlayerState[ProbableInfo]], transactions: List[SOCTransactions]): (Map[Int, PlayerState[ProbableInfo]], InventoryManager[ProbableInfo]) = {
    val newPossibleHands = possibleHands.calculateHands(transactions)
    val update = copy(possibleHands = newPossibleHands)
    (players.map{ case (i, ps) =>
        i -> ps.updateResources(newPossibleHands.probableHands(i))
      }, update)
  }

  override def playDevelopmentCard(players: Map[Int, PlayerState[ProbableInfo]], id: Int, card: DevelopmentCard): (Map[Int, PlayerState[ProbableInfo]], InventoryManager[ProbableInfo])  = {
    val newPossibleDevCards = possibleDevCards.playCard(id, card)
    updateDevCards(players, newPossibleDevCards)
  }

  override def buyDevelopmentCard(players: Map[Int, PlayerState[ProbableInfo]], id: Int, card: Option[DevelopmentCard]): (Map[Int, PlayerState[ProbableInfo]], InventoryManager[ProbableInfo])  = card match {
    case Some(dcard) =>
      val newPossibleDevCards = possibleDevCards.buyKnownCard(id, dcard)
      updateDevCards(players, newPossibleDevCards)

    case None =>
      val newPossibleDevCards = possibleDevCards.buyUnknownCard(id)
      updateDevCards(players, newPossibleDevCards)
  }

  private def updateDevCards(players: Map[Int, PlayerState[ProbableInfo]], possibleDevelopmentCards: PossibleDevelopmentCards): (Map[Int, PlayerState[ProbableInfo]], InventoryManager[ProbableInfo]) = {
    val update = copy(possibleDevCards = possibleDevelopmentCards)
    (players.map{ case (i, ps) =>
        val possDevCards = possibleDevelopmentCards(i)
        i -> ps.updateDevelopmentCard(
          possDevCards.playedDevCards,
          possDevCards.knownunplayedDevCards,
          possDevCards.unknownDevCards
        )
      }, update)
  }

  override def createInventory(position: Int): ProbableInfo = new ProbableInfoInventory(position)
}

case class NoInfoInventoryManager(implicit val gameRules: GameRules) extends InventoryManager[NoInfo] {

  override def updateResources(players: Map[Int, PlayerState[NoInfo]], transactions: List[SOCTransactions]): (Map[Int, PlayerState[NoInfo]], InventoryManager[NoInfo]) = {
    (players.mapValues(_.updateResources(transactions)), this)
  }


  override def playDevelopmentCard(players: Map[Int, PlayerState[NoInfo]], id: Int, card: DevelopmentCard):(Map[Int, PlayerState[NoInfo]], InventoryManager[NoInfo]) = {
    (players.map {
      case(`id`, ps) => id -> ps.updateDevelopmentCard(PlayDevelopmentCard(id, card))
      case (i, ps) => i -> ps
    }, this)
  }



  override def buyDevelopmentCard(players: Map[Int, PlayerState[NoInfo]], id: Int, card: Option[DevelopmentCard]): (Map[Int, PlayerState[NoInfo]], InventoryManager[NoInfo]) = {
    (players.map {
      case (`id`, ps) => id -> ps.updateDevelopmentCard(BuyDevelopmentCard(id, card))
      case (i, ps) => i -> ps
    }, this)
  }


  override def createInventory(position: Int): NoInfo = new NoInfoInventory(position)
}

trait InventoryManagerFactory[T <: Inventory[T]] {
  def createInventoryManager(implicit gameRules: GameRules): InventoryManager[T]
}

object InventoryManager {

  implicit val perfectInfoInventoryManagerFactory = new InventoryManagerFactory[PerfectInfo] {
    override def createInventoryManager(implicit gameRules: GameRules): InventoryManager[PerfectInfo] = PerfectInfoInventoryManager()
  }

  implicit val probableInfoInventoryManagerFactory = new InventoryManagerFactory[ProbableInfo] {
    override def createInventoryManager(implicit gameRules: GameRules): InventoryManager[ProbableInfo] = ProbableInfoInventoryManager(SOCPossibleHands.empty, PossibleDevelopmentCards.empty)
  }

  implicit val noInfoInventoryManagerFactory = new InventoryManagerFactory[NoInfo] {
    override def createInventoryManager(implicit gameRules: GameRules): InventoryManager[NoInfo] = NoInfoInventoryManager()
  }
}




