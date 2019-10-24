package soc.playerRepository

import soc.inventory.Inventory
import scala.collection.mutable.Map

class PlayerRepository {

  private val repo =  Map.empty[String, PlayerContext[_]]

  def addPlayer(context: PlayerContext[_]): Unit = {
    println(s"added player: ${context.name}")
    repo(context.name) = context
  }
  def contains(playerId: String) = repo.contains(playerId)
  def getPlayer(playerId: String): Option[PlayerContext[_]] = repo.get(playerId)

}
