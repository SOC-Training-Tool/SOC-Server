package soc.playerRepository

import soc.game.{GameState, MoveResult}
import soc.game.inventory.Inventory
import soc.storage.GameId

trait StateHandler[T <: Inventory[T]] {

  def setInitialState(gameId: GameId, initialState: GameState[T]): Unit

  def updateState(gameId: GameId, position: Int, moveResult: MoveResult): Unit

  def getState(gameId: GameId): GameState[T]

}