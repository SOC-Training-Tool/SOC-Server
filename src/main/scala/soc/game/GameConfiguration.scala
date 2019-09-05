package soc.game

import akka.actor.typed.ActorRef
import soc.akka.MoveResultProviderMessage.MoveResultProviderMessage
import soc.akka.{GameStateHolder, MoveResultProvider}
import soc.akka.messages.GameMessage
import soc.game.board.{BoardConfiguration, BoardGenerator, CatanBoard}
import soc.game.inventory.Inventory
import soc.game.inventory.InventoryHelperFactory
import soc.game.player.PlayerStateHelper
import soc.playerRepository.PlayerContext

case class GameConfiguration[GAME <: Inventory[GAME], PLAYERS <: Inventory[PLAYERS], BOARD <: BoardConfiguration](
  gameId: Int,
  boardConfig: BOARD,
  players: Map[Int, PlayerContext[GAME, _]],
  resultProvider: ActorRef[MoveResultProviderMessage[GAME]],
  moveRecorder: Option[ActorRef[GameMessage]],
  rules: GameRules)
  (implicit gameInventoryHelperFactory: InventoryHelperFactory[GAME],
    playersInventoryHelperFactory: InventoryHelperFactory[PLAYERS],
    boardGenerator: BoardGenerator[BOARD]) {

  val playerIds = players.keys.toSeq.sorted
  val playerNameIds = players.map { case (id, context) => (context.name, id)}.toSeq

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
    GameState[GAME](board = initBoard, players = PlayerStateHelper(playerNameIds)(gameInventoryHelperFactory, rules), bank = rules.initBank, devCardsDeck = rules.initDevCardAmounts.getTotal),
    playerIds.map { id => id -> GameState[PLAYERS](initBoard, PlayerStateHelper(playerNameIds)(playersInventoryHelperFactory, rules), bank = rules.initBank, devCardsDeck = rules.initDevCardAmounts.getTotal) }.toMap
  )
}