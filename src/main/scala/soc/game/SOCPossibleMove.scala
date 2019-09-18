package soc.game

import soc.game.board.{Edge, Vertex}
import soc.game.inventory.resources.CatanResourceSet
import soc.game.inventory.resources.CatanResourceSet.Resources

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

/**
  * CatanTypes for Possible Moves
  */

/**
  * Catan Move and Response
  */


/**
  * CatanTypes for Buildings
  */

