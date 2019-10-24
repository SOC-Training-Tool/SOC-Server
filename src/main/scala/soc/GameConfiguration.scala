package soc

import akka.actor.typed.ActorRef
import server.GameContext
import soc.behaviors.MoveResultProvider
import soc.behaviors.MoveResultProviderMessage.MoveResultProviderMessage
import soc.behaviors.messages.GameMessage
import soc.board.{BoardConfiguration, BoardGenerator, CatanBoard}
import soc.core.GameRules
import soc.inventory.{Inventory, InventoryHelperFactory}
import soc.playerRepository.PlayerContext
import soc.state.GameState
import soc.storage.GameId

case class GameConfiguration[GAME <: Inventory[GAME], BOARD <: BoardConfiguration](
  gameId: GameId,
  context: GameContext[GAME, BOARD],
  boardConfiguration: BOARD,
  players: Map[Int, PlayerContext[GAME]],
  resultProvider: MoveResultProvider[GAME],
  moveRecorder: Option[ActorRef[GameMessage]],
  rules: GameRules)
  (implicit gameInventoryHelperFactory: InventoryHelperFactory[GAME], boardGenerator: BoardGenerator[BOARD]) {

  val initBoard = boardGenerator(boardConfiguration)
  val playerIds = players.keys.toSeq.sorted
  val playerNameIds = players.toSeq.map { case (id, context) => (context.name, id)}
  val initState = GameState[GAME](board = initBoard, playerNameIds = playerNameIds, rules = rules)

}