package soc.akka

import akka.actor.PoisonPill
import akka.actor.typed.{ActorRef, ActorSystem}
import soc.akka.messages.{GameMessage, Terminate}
import soc.game.dice.NormalDice
import soc.game.player.moveSelector.PossibleMoveSelector
import soc.game._
import soc.game.board.{BaseBoardConfiguration, BaseCatanBoard}
import soc.game.inventory.Inventory.{NoInfo, PerfectInfo}
import soc.sql.MoveEntry
import soc.storage.MoveSaver
import soc.game.inventory._

import scala.concurrent.{Await, Future}
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Main extends App {

  implicit val random = new Random()

  val boardConfig = BaseCatanBoard.randomBoard

  val dice = NormalDice()

  type GAME = PerfectInfo
  type PLAYER = NoInfo
  type T = NoInfo

  import InventoryManager._
  import BaseCatanBoard._
  implicit val gameRules = GameRules()

  val dCardDeck: List[DevelopmentCard] = DevelopmentCardDeckBuilder.buildDeckByCardTypeAndAmount(
    Map(Knight -> Knight.initAmount,
      CatanPoint -> CatanPoint.initAmount,
      RoadBuilder -> RoadBuilder.initAmount,
      Monopoly -> Monopoly.initAmount,
      YearOfPlenty -> YearOfPlenty.initAmount)
  )

  val inMemoryMoveSaver = new MoveSaver {

    var moves: List[MoveEntry] = Nil
    override def saveMove(move: MoveEntry): Unit = moves = move :: moves
    override def toString = moves.mkString("\n")
  }
  val moveSaverActor: ActorRef[GameMessage] = ActorSystem(MoveSaverBehavior.moveSaverBehavior(inMemoryMoveSaver), "moveSaver")

  val randSelector = PossibleMoveSelector.randSelector[NoInfo]

  val players = Map(
    ("player0", 0) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player0"),
    ("player1", 1) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player1"),
    ("player2", 2) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player2"),
    ("player3", 3) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player3")
  )

  val randomMoveResultProvider = new RandomMoveResultProvider(dice, dCardDeck)

  val numGames = 200
  val averageGameLengthSeconds = 20

  val games = for {
    i <- 1 to numGames
    config = GameConfiguration[PerfectInfo, NoInfo, BaseBoardConfiguration](i, boardConfig, players, randomMoveResultProvider, None, gameRules)
  } yield ActorSystem(GameBehavior.gameBehavior(config), s"SettlersOfCatan$i").whenTerminated

  Await.result(Future.sequence(games), (numGames * averageGameLengthSeconds).seconds)

  println("gamesAreOver")

  println(inMemoryMoveSaver.moves.groupBy(_.gameId).map(_._2.length).sum.toDouble / 100.0)


  players.values.foreach(_ ! Terminate)
  moveSaverActor ! Terminate







}
