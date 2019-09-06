package soc.akka

import akka.actor.PoisonPill
import akka.actor.typed.{ActorRef, ActorSystem}
import soc.akka.messages.{GameMessage, Terminate}
import soc.game.dice.NormalDice
import soc.game.player.moveSelector.PossibleMoveSelector
import soc.game._
import soc.game.board.{BaseBoardConfiguration, BaseCatanBoard}
import soc.game.inventory.Inventory.{NoInfo, PerfectInfo}
import soc.game.inventory._
import soc.storage.aws.AWSMoveSaver

import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import soc.aws.client.CatanGameStoreClientFactory
import soc.game.inventory.resources.CatanResourceSet
import soc.game.inventory.InventoryManager._
import soc.simulation.SimulationQueue

object Main extends App {

  final val SAVE_MOVES: Boolean = false
  final val NUMBER_OF_BOARDS: Int = 1
  final val GAMES_PER_BOARD: Int = 1
  final val GAMES_AT_A_TIME: Int = 1

  implicit val random = new Random()

  val boardConfig = BaseCatanBoard.randomBoard

  val dice = NormalDice()

  import BaseCatanBoard._
  implicit val gameRules = GameRules()

  val dCardDeck: List[DevelopmentCard] = DevelopmentCardDeckBuilder.buildDeckByCardTypeAndAmount(
    Map(Knight -> Knight.initAmount,
      CatanPoint -> CatanPoint.initAmount,
      RoadBuilder -> RoadBuilder.initAmount,
      Monopoly -> Monopoly.initAmount,
      YearOfPlenty -> YearOfPlenty.initAmount)
  )

  import CatanResourceSet._
  import io.circe.generic.auto._
  
  
  var moveSaverOption: Option[ActorRef[GameMessage]] = None
  if (SAVE_MOVES) {
    val awsGameSaver = new AWSMoveSaver[BaseBoardConfiguration](CatanGameStoreClientFactory.createClient())
    val moveSaverActor: ActorRef[GameMessage] = ActorSystem(MoveSaverBehavior.moveSaverBehavior(awsGameSaver), "moveSaver")
    moveSaverOption = Option(moveSaverActor)
  }

  val randSelector = PossibleMoveSelector.randSelector[NoInfo]

  val players = Map(
    ("randomPlayer", 0) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player0"),
    ("randomPlayer", 1) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player1"),
    ("randomPlayer", 2) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player2"),
    ("randomPlayer", 3) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player3")
  )

  val moveProvider = new RandomMoveResultProvider(dice, dCardDeck)
  val randomMoveResultProvider = ActorSystem(MoveResultProvider.moveResultProvider(moveProvider), "resultProvider")


 SimulationQueue[PerfectInfo, NoInfo, BaseBoardConfiguration](
    players,
    randomMoveResultProvider,
    moveSaverOption,
    gameRules,
    NUMBER_OF_BOARDS,
    GAMES_PER_BOARD,
    GAMES_AT_A_TIME).startGames
}
