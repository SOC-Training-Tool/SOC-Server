package soc.game

import soc.game.inventory.developmentCard.DevCardInventory
import soc.game.inventory.developmentCard.DevCardInventory.PlayedInventory
import soc.game.inventory.resources.CatanResourceSet
import soc.game.inventory.resources.CatanResourceSet.Resources

case class GameRules(
  pointsToWin: Int = GameRules.POINTS_TO_WIN,
  initBank: Resources = GameRules.INITIAL_BANK,
  initDevCardAmounts: PlayedInventory = GameRules.INITIAL_DEV_AMOUNTS,
  numSettlements: Int = GameRules.NUM_SETTLEMENTS,
  numCities: Int = GameRules.NUM_CITIES,
  numRoads: Int = GameRules.NUM_ROADS
)

object GameRules {

  val default = GameRules()

  val POINTS_TO_WIN = 10
  val INITIAL_BANK = CatanResourceSet(19, 19, 19, 19, 19)
  val INITIAL_DEV_AMOUNTS = DevCardInventory(14, 5, 2, 2, 2)
  val NUM_SETTLEMENTS = 5
  val NUM_CITIES = 4
  val NUM_ROADS = 15
}
