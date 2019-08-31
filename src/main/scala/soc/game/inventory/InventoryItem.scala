package soc.game.inventory

import soc.game.inventory.resources.CatanResourceSet

sealed trait InventoryItem

sealed trait Port {val id: Int}
sealed abstract class Resource(val id: Int, val name: String) extends InventoryItem

object Resource {
  val list: List[Resource] = List(Wood, Sheep, Wheat, Brick, Ore)
}

case object Wood extends Resource(1, "Wood") with Port
case object Sheep extends Resource(2, "Sheep") with Port
case object Wheat extends Resource(3, "Wheat") with Port
case object Brick extends Resource(4, "Brick") with Port
case object Ore extends Resource(5, "Ore") with Port

case object Misc extends Port {val id = 0}

sealed trait DevelopmentCard extends InventoryItem {
  val initAmount: Int
}
case object Knight extends DevelopmentCard {
  override val initAmount: Int = 14
}
case object CatanPoint extends DevelopmentCard {
  override val initAmount: Int = 5
}
case object RoadBuilder extends DevelopmentCard {
  override val initAmount: Int = 2
}
case object Monopoly extends DevelopmentCard {
  override val initAmount: Int = 2
}
case object YearOfPlenty extends DevelopmentCard {
  override val initAmount: Int = 2
}

sealed trait CatanBuilding extends InventoryItem {
  val playerId: Int
}
sealed trait VertexBuilding extends CatanBuilding
sealed trait EdgeBuilding extends CatanBuilding

case class Settlement(playerId: Int) extends VertexBuilding
case class City(playerId: Int) extends VertexBuilding
case class Road(playerId: Int) extends EdgeBuilding

object Settlement {
  val cost = CatanResourceSet(1, 0, 1, 1, 1)
}
object City {
  val cost = CatanResourceSet(0, 3, 0, 2, 0)
}
object Road {
  val cost = CatanResourceSet(1, 0, 0, 0, 1)
}
object DevelopmentCard {
  val list: List[DevelopmentCard] = List(Knight, CatanPoint, RoadBuilder, Monopoly, YearOfPlenty)
  val cost = CatanResourceSet(0, 1, 1, 1, 0)
}