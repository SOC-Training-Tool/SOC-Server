package soc.playerRepository

import soc.inventory.Inventory
import soc.moves.MoveResult
import soc.state.GameState
import soc.storage.GameId

trait StateHandler[T <: Inventory[T]] {

  def setInitialState(gameId: GameId, initialState: GameState[T]): Unit

  def updateState(gameId: GameId, position: Int, moveResult: MoveResult): Unit

  def getState(gameId: GameId): GameState[T]

}