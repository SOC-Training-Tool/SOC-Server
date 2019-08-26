package soc.game.inventory.resources

import CatanResourceSet.Resources

case class DiscardedCardsMapBuilder private(playersToDiscard: Seq[Int], cardsToDiscard: Map[Int, Resources] = Map.empty) {

  val waitingFor = playersToDiscard.filterNot(cardsToDiscard.contains)
  val expectingDiscard = !waitingFor.isEmpty

  def addDiscard(playerId: Int, cards: Resources): DiscardedCardsMapBuilder = copy(cardsToDiscard = cardsToDiscard + (playerId -> cards))

}
