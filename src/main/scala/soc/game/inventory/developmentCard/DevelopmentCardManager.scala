package soc.game.inventory.developmentCard

import soc.game.inventory._
import soc.game.inventory.developmentCard.DevCardInventory.DevelopmentCardSet

import scala.util.Random

case class DevCardInventory[T: Numeric](kn: T = 0, pt: T = 0, rb: T = 0, mp: T = 0, yp: T = 0) extends CatanSet[DevelopmentCard, T]{

  override protected val implWrap: NumericWrapper = NumericWrapper()

  override protected def _copy(map: Map[DevelopmentCard, T]): DevelopmentCardSet[T] = DevCardInventory(
    map.get(Knight).getOrElse(implWrap.wrapped.zero),
    map.get(CatanPoint).getOrElse(implWrap.wrapped.zero),
    map.get(RoadBuilder).getOrElse(implWrap.wrapped.zero),
    map.get(Monopoly).getOrElse(implWrap.wrapped.zero),
    map.get(YearOfPlenty).getOrElse(implWrap.wrapped.zero)
  )

  override val amountMap: Map[DevelopmentCard, T] = Map(Knight -> kn, CatanPoint -> pt, RoadBuilder -> rb, Monopoly -> mp, YearOfPlenty -> yp)
}

object DevCardInventory {
  type DevelopmentCardSet[T] = CatanSet[DevelopmentCard, T]

  type PlayedInventory = DevelopmentCardSet[Int]
  type UnplayedInventory = DevelopmentCardSet[Double]


  def empty[T: Numeric]: DevelopmentCardSet[T] = {
    val num = implicitly[Numeric[T]]
    DevCardInventory(num.zero, num.zero, num.zero, num.zero, num.zero)
  }

  implicit def toInventory[T](map: Map[DevelopmentCard, T])(implicit num: Numeric[T]): DevelopmentCardSet[T] = {
    DevCardInventory[T](
      map.get(Knight).getOrElse(num.zero),
      map.get(CatanPoint).getOrElse(num.zero),
      map.get(RoadBuilder).getOrElse(num.zero),
      map.get(Monopoly).getOrElse(num.zero),
      map.get(YearOfPlenty).getOrElse(num.zero)
    )
  }
}

class DevelopmentCardManager(kn: Int, po: Int, mp: Int, rb: Int, yp: Int)(implicit random: Random) {

  def buildDevelopmentCardDeck: List[DevelopmentCard] = random.shuffle {
    (1 to kn).map(_ => Knight).toList :::
      (1 to po).map(_ => CatanPoint).toList :::
      (1 to mp).map(_ => Monopoly).toList :::
      (1 to rb).map(_ => RoadBuilder).toList :::
      (1 to yp).map(_ => YearOfPlenty).toList
  }
}

sealed trait DevCardTransaction

case class BuyDevelopmentCard(playerId: Int, card: Option[DevelopmentCard]) extends DevCardTransaction
case class PlayDevelopmentCard(playerId: Int, card: DevelopmentCard) extends DevCardTransaction