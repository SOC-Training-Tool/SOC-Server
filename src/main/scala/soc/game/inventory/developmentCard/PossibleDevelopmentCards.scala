package soc.game.inventory.developmentCard

import soc.game.GameRules
import soc.game.inventory.DevelopmentCard
import soc.game.inventory.developmentCard.DevCardInventory.{DevelopmentCardSet, PlayedInventory, UnplayedInventory}

case class PossibleDevCardsHands(
  playedDevCards: PlayedInventory = DevCardInventory.empty[Int],
  knownunplayedDevCards: PlayedInventory = DevCardInventory.empty[Int],
  numUnknownDevCards: Int = 0,
  unknownDevCards: UnplayedInventory = DevCardInventory.empty[Double])

case class PossibleDevelopmentCards(cards: Map[Int, PossibleDevCardsHands] = Map.empty)(implicit gameRules: GameRules) {

  def apply(player: Int) = cards.get(player).getOrElse(PossibleDevCardsHands())

  lazy val knownCards = cards.map { case (_, pdev) =>
    pdev.playedDevCards.add(pdev.knownunplayedDevCards)
  }.foldLeft(DevCardInventory.empty[Int])(_.add(_))

  lazy val prob = {
    val left = gameRules.initDevCardAmounts.subtract(knownCards)
    DevCardInventory.toInventory(left.amountMap.map { case (card, amt) =>
      card -> amt.toDouble / left.getTotal.toDouble
    })
  }

  private lazy val updateUnknownDevCards: PossibleDevelopmentCards = copy(
    cards.map {
      case (p, hand) =>
        val numUnknownCards = hand.numUnknownDevCards
        p -> hand.copy(unknownDevCards = (1 to numUnknownCards).foldLeft(DevCardInventory.empty[Double]) { case (newSet, _) => newSet.add(prob) })
    }
  )

  def buyKnownCard(player: Int, card: DevelopmentCard): PossibleDevelopmentCards =
    cards.get(player).fold(copy(cards + (player -> PossibleDevCardsHands())))(_ => this).copy(
      cards.map {
        case (`player`, hand) => player -> hand.copy(knownunplayedDevCards = hand.knownunplayedDevCards.add(1, card))
        case (p, hand) => p -> hand
      }
    ).updateUnknownDevCards

  def buyUnknownCard(player: Int): PossibleDevelopmentCards =
    cards.get(player).fold(copy(cards + (player -> PossibleDevCardsHands())))(_ => this).copy(
      cards.map {
        case (`player`, hand) => player -> hand.copy(numUnknownDevCards = hand.numUnknownDevCards + 1)
        case (p, hand) => p -> hand
      }
    ).updateUnknownDevCards

  def playCard(player: Int, card: DevelopmentCard): PossibleDevelopmentCards = copy(
    cards.map {
      case (`player`, hand) =>
        val addPlayedCard = hand.copy(playedDevCards = hand.playedDevCards.add(1, card))
        player -> {
          if (addPlayedCard.knownunplayedDevCards.contains(card)) {
            addPlayedCard.copy(knownunplayedDevCards = addPlayedCard.knownunplayedDevCards.subtract(1, card))
          } else addPlayedCard.copy(numUnknownDevCards = addPlayedCard.numUnknownDevCards - 1)
        }
      case (p, hand) => p -> hand
    }
  ).updateUnknownDevCards


}

object PossibleDevelopmentCards {
  def empty(implicit gameRules: GameRules) = PossibleDevelopmentCards()
}
