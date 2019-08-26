package soc.game

import akka.actor.typed.ActorRef
import soc.akka.{GameStateHolder, MoveResultProvider}
import soc.akka.messages.GameMessage
import soc.game.board.{BoardConfiguration, BoardGenerator, CatanBoard}
import soc.game.inventory.Inventory
import soc.game.inventory.InventoryManagerFactory
import soc.game.player.PlayerStateManager

case class GameConfiguration[GAME <: Inventory[GAME], PLAYERS <: Inventory[PLAYERS], BOARD <: BoardConfiguration](
  gameId: Int,
  boardConfig: BOARD,
  players: Map[(String, Int), ActorRef[GameMessage]],
  resultProvider: MoveResultProvider[GAME],
  moveRecorder: Option[ActorRef[GameMessage]],
  rules: GameRules)
  (implicit gameInventoryManagerFactory: InventoryManagerFactory[GAME],
    playersInventoryManagerFactory: InventoryManagerFactory[PLAYERS],
    boardGenerator: BoardGenerator[BOARD]) {

  val playerIds = players.keys.map(_._2).toSeq.sorted
  val playerRefs: Map[Int, ActorRef[GameMessage]] = players.map {
    case ((_, id), ref) => (id, ref)
  }
  val firstPlayerId = playerIds.min
  val lastPlayerId = playerIds.max

  def nextPlayer(playerId: Int): Int = {
    val indexOf = playerIds.indexOf(playerId)
    playerIds.drop(indexOf + 1).headOption.getOrElse(firstPlayerId)
  }

  def previousPlayer(playerId: Int): Int = {
    val indexOf = playerIds.indexOf(playerId)
    playerIds.dropRight(playerIds.length - indexOf).lastOption.getOrElse(lastPlayerId)
  }

  val initBoard = boardGenerator.apply(boardConfig)

  val initStates: GameStateHolder[GAME, PLAYERS] = GameStateHolder(
    GameState[GAME](board = initBoard, players = PlayerStateManager(players.keys.toSeq)(gameInventoryManagerFactory, rules), bank = rules.initBank, devCardsDeck = rules.initDevCardAmounts.getTotal),
    playerIds.map { id => id -> GameState[PLAYERS](initBoard, PlayerStateManager(players.keys.toSeq)(playersInventoryManagerFactory, rules), bank = rules.initBank, devCardsDeck = rules.initDevCardAmounts.getTotal) }.toMap
  )
}