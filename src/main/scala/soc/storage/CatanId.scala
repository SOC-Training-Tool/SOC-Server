package soc.storage

sealed abstract class GameType(val description: String)
case object SimulatedGame extends GameType("Simulation")

case class GameId(gameType: GameType, batchId: String, iteration: Int) {
  val key: String = s"${gameType.description}-$batchId-$iteration"
}