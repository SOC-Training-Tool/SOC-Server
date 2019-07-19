package soc.akka

import akka.actor.PoisonPill
import akka.actor.typed.{ActorRef, ActorSystem}
import soc.akka.messages.Terminate
import soc.game.dice.NormalDice
import soc.game.player.moveSelector.PossibleMoveSelector
import soc.game._
import soc.game.board.BaseCatanBoard

import scala.concurrent.{Await, Future}
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Main extends App {

  implicit val random = new Random()

  val board = BaseCatanBoard.randomBoard

  val dice = NormalDice()

  val randSelector = PossibleMoveSelector { case (_, moves: Iterator[CatanMove]) =>
    val (a, b) = moves.duplicate
    b.drop(random.nextInt(a.length)).next()
  }

  val dCardDeck: List[DevelopmentCard] = DevelopmentCardDeckBuilder.buildDeckByCardTypeAndAmount(
    Map(Knight -> Knight.initAmount,
      CatanPoint -> CatanPoint.initAmount,
      RoadBuilder -> RoadBuilder.initAmount,
      Monopoly -> Monopoly.initAmount,
      YearOfPlenty -> YearOfPlenty.initAmount)
  )


  val players = Map(
    ("player0", 0) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player0"),
    ("player1", 1) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player1"),
    ("player2", 2) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player2"),
    ("player3", 3) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player3")
  )

  val numGames = 100
  val averageGameLengthSeconds = 10

  val games = for {
    i <- 1 to numGames
  } yield ActorSystem(GameBehavior.gameBehavior(i, board, dice, dCardDeck, players), s"SettlersOfCatan$i").whenTerminated

  Await.result(Future.sequence(games), (numGames * averageGameLengthSeconds).seconds)

  println("gamesAreOver")

  players.values.foreach(_ ! Terminate)







}
