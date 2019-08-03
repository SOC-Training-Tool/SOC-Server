package soc.storage

import soc.sql.MoveEntry

trait MoveSaver {

  def saveMove(move: MoveEntry): Unit

}
