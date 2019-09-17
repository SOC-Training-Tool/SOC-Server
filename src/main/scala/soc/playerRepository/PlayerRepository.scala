package soc.playerRepository

import soc.game.inventory.Inventory
import scala.collection.mutable.Map

class PlayerRepository[GAME <: Inventory[GAME]] {

  private val repo =  Map.empty[String, PlayerContext[GAME, _]]

  def addPlayer(context: PlayerContext[GAME, _]): Unit = {
    println(s"added player: ${context.name}")
    repo(context.name) = context
  }

  def getPlayer(playerId: String): Option[PlayerContext[GAME, _]] = repo.get(playerId)

}
