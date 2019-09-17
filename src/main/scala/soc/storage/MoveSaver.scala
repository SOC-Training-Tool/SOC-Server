package soc.storage

import soc.game.board.BoardConfiguration

trait MoveSaver[BOARD <: BoardConfiguration] {

  def saveMove(move: MoveEntry): Unit

  def saveGame(gameId: GameId, initBoard: BOARD, players: Map[(String, Int), Int]): Unit

}
