package soc

import soc.board.{BoardConfiguration, BoardGenerator}
import soc.core.GameRules
import soc.inventory.{Inventory, InventoryHelperFactory}
import soc.state.GameState
import soc.storage.GameId

case class GameConfiguration[GAME <: Inventory[GAME], BOARD <: BoardConfiguration](
  gameId: GameId,
  boardConfiguration: BOARD,
  players: Map[Int, String],
  rules: GameRules)
  (implicit gameInventoryHelperFactory: InventoryHelperFactory[GAME], boardGenerator: BoardGenerator[BOARD]) {

  val initBoard = boardGenerator(boardConfiguration)
  val playerIds = players.keys.toSeq.sorted
  val playerNameIds = players.toSeq.map { case (id, name) => (name, id)}

  val initState = GameState[GAME](board = initBoard, playerNameIds = playerNameIds, rules = rules)

}