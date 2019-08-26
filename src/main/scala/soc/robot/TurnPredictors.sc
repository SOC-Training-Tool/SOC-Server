import soc.game._
import soc.game.board.{BaseCatanBoard, CatanBoard, Vertex}
import soc.game.inventory.resources.CatanResourceSet
import soc.game.player.PlayerState

val hexes = List(
  ResourceHex(Wheat, Roll(6)),
  ResourceHex(Ore, Roll(2)),
  ResourceHex(Sheep, Roll(5)),
  ResourceHex(Ore, Roll(8)),
  ResourceHex(Wood, Roll(4)),
  ResourceHex(Brick, Roll(11)),
  ResourceHex(Sheep, Roll(12)),
  ResourceHex(Ore, Roll(9)),
  ResourceHex(Sheep, Roll(10)),
  ResourceHex(Brick, Roll(8)),
  Desert,
  ResourceHex(Wheat, Roll(3)),
  ResourceHex(Sheep, Roll(9)),
  ResourceHex(Brick, Roll(10)),
  ResourceHex(Wood, Roll(3)),
  ResourceHex(Wood, Roll(6)),
  ResourceHex(Wheat, Roll(5)),
  ResourceHex(Wood, Roll(4)),
  ResourceHex(Wheat, Roll(11))
)

val ports: List[Port] = List(Misc, Ore, Misc, Wheat, Misc, Brick, Wood, Sheep, Misc)

var board: CatanBoard = BaseCatanBoard(hexes, ports)

board = board.buildSettlement(Vertex(23), 1)._2
board = board.buildSettlement(Vertex(45), 1)._2
board = board.buildSettlement(Vertex(17), 1)._2
board = board.buildSettlement(Vertex(41), 1)._2
board = board.buildSettlement(Vertex(49), 1)._2

board = board.buildSettlement(Vertex(52), 2)._2
board = board.buildCity(Vertex(52), 2)._2
board = board.buildSettlement(Vertex(8), 2)._2
board = board.buildCity(Vertex(8), 2)._2
board = board.buildSettlement(Vertex(34), 2)._2
board = board.buildCity(Vertex(34), 2)._2

val player1 = PlayerState("greg", 1)
val player2 = PlayerState("dyl", 2)

var state = GameState(board, List(player1, player2))

def getRollCombinations(state: GameState, playerId: Int, goal: CatanResourceSet[Int], numRolls: Int): List[Roll] = {

  val rolls = (2 to 12).filterNot(_ == 7).map(Roll)
  var stack = rolls.map(List(_))

}







