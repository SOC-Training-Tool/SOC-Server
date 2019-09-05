package soc.playerRepository

import soc.game.inventory.Inventory

class PlayerRepository[GAME <: Inventory[GAME]] {

  def addPlayer(): Unit = ()

  def getPlayer(playerId: String): PlayerContext[GAME, _] = {
    null
  }

}
