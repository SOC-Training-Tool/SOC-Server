package soc.game

import soc.game.board.{Edge, Vertex}
import soc.game.resources.CatanResourceSet.Resources
import soc.game.resources.{CatanResourceSet, Steal}

case class TurnState(
  canPlayDevCard: Boolean = true,
  canRollDice: Boolean = true
)

/**
  * CatanTypes for Resources and Hexes
  */

case class Roll(number: Int) {
  val dots: Int = 6 - Math.abs(7 - number)
  val prob: Double = dots.toDouble / 36.0
}

sealed trait Port
sealed abstract class Resource(val res: Int, val name: String)

object Resource {
  val list: List[Resource] = List(Wood, Sheep, Wheat, Brick, Ore)
}

case object Wood extends Resource(1, "Wood") with Port
case object Sheep extends Resource(2, "Sheep") with Port
case object Wheat extends Resource(3, "Wheat") with Port
case object Brick extends Resource(4, "Brick") with Port
case object Ore extends Resource(5, "Ore") with Port

case object Misc extends Port

sealed trait Hex {
  val getResourceAndNumber: Option[(Resource, Roll)]
  val getResource: Option[Resource]
  val getNumber: Option[Roll]
}
case class ResourceHex(resource: Resource, number: Roll) extends Hex {
  override val getResourceAndNumber: Option[(Resource, Roll)] = Some((resource, number))
  override val getResource: Option[Resource] = Some(resource)
  override val getNumber: Option[Roll] = Some(number)
}
case object Desert extends Hex {
  override val getResourceAndNumber: Option[(Resource, Roll)] = None
  override val getResource: Option[Resource] = None
  override val getNumber: Option[Roll] = None
}

sealed trait DevelopmentCard {
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

/**
  * CatanTypes for Possible Moves
  */
case class RobberLocationsAndSteal(node: Int, playerStole: Option[Int])

/**
  * Catan Move and Response
  */


/**
  * CatanTypes for Buildings
  */

sealed trait CatanBuilding {
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
  val cost = CatanResourceSet(0, 1, 1, 1, 0)
}