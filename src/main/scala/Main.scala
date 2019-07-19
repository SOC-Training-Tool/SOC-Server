//import akka.actor.{Actor, ActorSystem}
//import log.SystemLog
//import soc.actors.{GameActor, PlayerActor}
//import soc.game.board.BaseCatanBoard
//import soc.game.dice.NormalDice
//import soc.game.messages.{AddPlayer, StartGame}
//import soc.game.player.CatanMove
//import soc.game.player.moveSelector.PossibleMoveSelector
//
//import scala.util.Random
//
//object Main extends App {
//
//  val system: ActorSystem = ActorSystem("SettlersOfCatan")
//
//  implicit val random = new Random()
//
//  val board = BaseCatanBoard.randomBoard
//  val dice = NormalDice()
//  val log = new SystemLog
//
//  val game = system.actorOf(GameActor.props(board, dice, log))
//
//  val randSelector: List[CatanMove] => CatanMove = {
//    (moves: List[CatanMove]) => moves(random.nextInt(moves.length))
//  }
//
//  val player1 = system.actorOf(PlayerActor.props("player1", 0, PossibleMoveSelector(randSelector)))
//  val player2 = system.actorOf(PlayerActor.props("player2", 1, PossibleMoveSelector(randSelector)))
//  val player3 = system.actorOf(PlayerActor.props("player3", 2, PossibleMoveSelector(randSelector)))
//  val player4 = system.actorOf(PlayerActor.props("player4", 3, PossibleMoveSelector(randSelector)))
//
//  game.tell(AddPlayer(0, "player1", player1), null)
//  game.tell(AddPlayer(1, "player2", player2), null)
//  game.tell(AddPlayer(2, "player3", player3), null)
//  game.tell(AddPlayer(3, "player4", player4), null)
//
//  game.tell(StartGame, null)
//
//}
