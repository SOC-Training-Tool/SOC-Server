package soc.storage

import soc.board.BoardConfiguration

trait MoveSaver[BOARD <: BoardConfiguration] {

  def saveMove(move: MoveEntry): Unit

  def saveGame(gameId: GameId, initBoard: BOARD, players: Map[(String, Int), Int]): Unit

}
