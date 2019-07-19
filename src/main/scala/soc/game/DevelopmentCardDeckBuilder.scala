package soc.game

import scala.util.Random


object DevelopmentCardDeckBuilder {
  def buildDeckByCardTypeAndAmount(cardAmountMap: Map[DevelopmentCard, Int])(implicit random: Random): List[DevelopmentCard] = {
    random.shuffle(cardAmountMap.flatMap { case (card, amount) => (1 to amount).map( _ => card) }).toList
  }
}