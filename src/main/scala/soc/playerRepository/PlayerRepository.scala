package soc.playerRepository

import soc.game.inventory.Inventory
import scala.collection.mutable.Map

class PlayerRepository {

  private val repo =  Map.empty[String, PlayerContext[_, _]]

  def addPlayer(context: PlayerContext[_, _]): Unit = {
    println(s"added player: ${context.name}")
    repo(context.name) = context
  }
  def contains(playerId: String) = repo.contains(playerId)
  def getPlayer(playerId: String): Option[PlayerContext[_, _]] = repo.get(playerId)

}
