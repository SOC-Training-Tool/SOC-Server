package soc.game

import soc.game.CatanMove.{BuildCity, BuildRoad, BuildSettlement, EndTurnMove, InitialPlacementMove, PortTrade, RoadBuilderMove, RollDiceMove, YearOfPlentyMove}

object GameStateTransitions {

//  case class StateWithProbability(state: GameState, prob: Double = 1.0)
//  def getPossibleStatesForMove(gameState: GameState, playerId: Int, move: CatanMove): Iterator[StateWithProbability] = move match {
//
//    case InitialPlacementMove(first, settlement, road) => Iterator(StateWithProbability(gameState.initialPlacement(playerId, first, settlement, road)))
//    case BuildRoad(edge) => Iterator(StateWithProbability(gameState.buildRoad(playerId, edge)))
//    case BuildSettlement(vertex) => Iterator(StateWithProbability(gameState.buildSettlement(playerId, vertex)))
//    case BuildCity(vertex) => Iterator(StateWithProbability(gameState.buildCity(playerId, vertex)))
//    case PortTrade(give, get) => Iterator(StateWithProbability(gameState.portTrade(playerId, give, get)))
//
//    case YearOfPlentyMove(res1, res2) => Iterator(StateWithProbability(gameState.playYearOfPlenty(playerId, res1, res2)))
//    case RoadBuilderMove(road1, road2) => Iterator(StateWithProbability(gameState.playRoadBuilder(playerId, road1, road2)))
//    case EndTurnMove => Iterator(StateWithProbability(gameState.endTurn(playerId)))
//
//    case RollDiceMove => (2 to 12).toIterator.flatMap {
//      case r if r != 7 =>
//        val roll = Roll(r)
//        Iterator(StateWithProbability(gameState.rollDice(playerId, roll), roll.prob))
//      case 7 =>
//        //CatanPossibleMoves(this, playerId).getPossibleDiscards()
//        Nil
//
//    }
//
//  }

}
