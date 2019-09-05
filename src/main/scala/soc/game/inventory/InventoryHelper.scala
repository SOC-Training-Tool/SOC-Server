package soc.game.inventory

import soc.game.GameRules
import soc.game.inventory.Inventory._
import soc.game.inventory.developmentCard.{BuyDevelopmentCard, PlayDevelopmentCard, PossibleDevelopmentCards}
import soc.game.inventory.resources.{PossibleHands, SOCPossibleHands, SOCTransactions}
import soc.game.player.PlayerState

trait InventoryHelper[T <: Inventory[T]] {

  implicit val gameRules: GameRules

  def updateResources(players: Map[Int, PlayerState[T]], transactions: List[SOCTransactions]): (Map[Int, PlayerState[T]], InventoryHelper[T])
  def playDevelopmentCard(players: Map[Int, PlayerState[T]], id: Int, card: DevelopmentCard): (Map[Int, PlayerState[T]], InventoryHelper[T])
  def buyDevelopmentCard(players: Map[Int, PlayerState[T]], id: Int, card: Option[DevelopmentCard]): (Map[Int, PlayerState[T]], InventoryHelper[T])
  def createInventory(position: Int): T
}

case class PerfectInfoInventoryHelper(implicit val gameRules: GameRules) extends InventoryHelper[PerfectInfo] {

  override def updateResources(players: Map[Int, PlayerState[PerfectInfo]], transactions: List[SOCTransactions]): (Map[Int, PlayerState[PerfectInfo]], InventoryHelper[PerfectInfo]) = {
    (players.mapValues(_.updateResources(transactions)), this)
  }

  override def playDevelopmentCard(players: Map[Int, PlayerState[PerfectInfo]], id: Int, card: DevelopmentCard): (Map[Int, PlayerState[PerfectInfo]], InventoryHelper[PerfectInfo]) = {
    (players.map {
        case(`id`, ps) => id -> ps.updateDevelopmentCard(PlayDevelopmentCard(id, card))
        case (i, ps) => i -> ps
      }, this)
  }

  override def buyDevelopmentCard(players: Map[Int, PlayerState[PerfectInfo]], id: Int, card: Option[DevelopmentCard]): (Map[Int, PlayerState[PerfectInfo]], InventoryHelper[PerfectInfo]) = {
    (players.map {
      case(`id`, ps) => id -> ps.updateDevelopmentCard(BuyDevelopmentCard(id, card))
      case (i, ps) => i -> ps
    }, this)
  }

  override def createInventory(position: Int): PerfectInfo = new PerfectInfoInventory(position)
}

case class ProbableInfoInventoryHelper(
  possibleHands: PossibleHands,
  possibleDevCards: PossibleDevelopmentCards)
  (implicit val gameRules: GameRules) extends InventoryHelper[ProbableInfo] {

  override def updateResources(players: Map[Int, PlayerState[ProbableInfo]], transactions: List[SOCTransactions]): (Map[Int, PlayerState[ProbableInfo]], InventoryHelper[ProbableInfo]) = {
    val newPossibleHands = possibleHands.calculateHands(transactions)
    val update = copy(possibleHands = newPossibleHands)
    (players.map{ case (i, ps) =>
        i -> ps.updateResources(newPossibleHands.probableHands(i))
      }, update)
  }

  override def playDevelopmentCard(players: Map[Int, PlayerState[ProbableInfo]], id: Int, card: DevelopmentCard): (Map[Int, PlayerState[ProbableInfo]], InventoryHelper[ProbableInfo])  = {
    val newPossibleDevCards = possibleDevCards.playCard(id, card)
    updateDevCards(players, newPossibleDevCards)
  }

  override def buyDevelopmentCard(players: Map[Int, PlayerState[ProbableInfo]], id: Int, card: Option[DevelopmentCard]): (Map[Int, PlayerState[ProbableInfo]], InventoryHelper[ProbableInfo])  = card match {
    case Some(dcard) =>
      val newPossibleDevCards = possibleDevCards.buyKnownCard(id, dcard)
      updateDevCards(players, newPossibleDevCards)

    case None =>
      val newPossibleDevCards = possibleDevCards.buyUnknownCard(id)
      updateDevCards(players, newPossibleDevCards)
  }

  private def updateDevCards(players: Map[Int, PlayerState[ProbableInfo]], possibleDevelopmentCards: PossibleDevelopmentCards): (Map[Int, PlayerState[ProbableInfo]], InventoryHelper[ProbableInfo]) = {
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

case class NoInfoInventoryHelper(implicit val gameRules: GameRules) extends InventoryHelper[NoInfo] {

  override def updateResources(players: Map[Int, PlayerState[NoInfo]], transactions: List[SOCTransactions]): (Map[Int, PlayerState[NoInfo]], InventoryHelper[NoInfo]) = {
    (players.mapValues(_.updateResources(transactions)), this)
  }


  override def playDevelopmentCard(players: Map[Int, PlayerState[NoInfo]], id: Int, card: DevelopmentCard):(Map[Int, PlayerState[NoInfo]], InventoryHelper[NoInfo]) = {
    (players.map {
      case(`id`, ps) => id -> ps.updateDevelopmentCard(PlayDevelopmentCard(id, card))
      case (i, ps) => i -> ps
    }, this)
  }



  override def buyDevelopmentCard(players: Map[Int, PlayerState[NoInfo]], id: Int, card: Option[DevelopmentCard]): (Map[Int, PlayerState[NoInfo]], InventoryHelper[NoInfo]) = {
    (players.map {
      case (`id`, ps) => id -> ps.updateDevelopmentCard(BuyDevelopmentCard(id, card))
      case (i, ps) => i -> ps
    }, this)
  }


  override def createInventory(position: Int): NoInfo = new NoInfoInventory(position)
}

trait InventoryHelperFactory[T <: Inventory[T]] {
  def createInventoryHelper(implicit gameRules: GameRules): InventoryHelper[T]
}

object InventoryHelper {

  implicit val perfectInfoInventoryManagerFactory = new InventoryHelperFactory[PerfectInfo] {
    override def createInventoryHelper(implicit gameRules: GameRules): InventoryHelper[PerfectInfo] = PerfectInfoInventoryHelper()
  }

  implicit val probableInfoInventoryManagerFactory = new InventoryHelperFactory[ProbableInfo] {
    override def createInventoryHelper(implicit gameRules: GameRules): InventoryHelper[ProbableInfo] = ProbableInfoInventoryHelper(SOCPossibleHands.empty, PossibleDevelopmentCards.empty)
  }

  implicit val noInfoInventoryManagerFactory = new InventoryHelperFactory[NoInfo] {
    override def createInventoryHelper(implicit gameRules: GameRules): InventoryHelper[NoInfo] = NoInfoInventoryHelper()
  }
}




