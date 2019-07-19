package soc.game.resources

import soc.game.resources.CatanResourceSet.Resources

case class DiscardedCardsMapBuilder private(playersToDiscard: List[Int], cardsToDiscard: Map[Int, Resources] = Map.empty) {

  val waitingFor = playersToDiscard.filterNot(cardsToDiscard.contains)
  val expectingDiscard = !waitingFor.isEmpty

  def addDiscard(playerId: Int, cards: Resources): DiscardedCardsMapBuilder = copy(cardsToDiscard = cardsToDiscard + (playerId -> cards))

}
