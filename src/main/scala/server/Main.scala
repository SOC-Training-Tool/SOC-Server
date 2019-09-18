package server

// TODO: Clean up imports
import java.util.logging.Logger

import io.grpc.{Server, ServerBuilder}
import io.grpc.stub.StreamObserver
import soc.protos.game.{CatanServerGrpc, CreateGameRequest, CreateGameResponse, StartGameRequest, StartGameResponse, MoveRequest, MoveResponse, SubscribeRequest, GameUpdate}

import scala.concurrent.{ExecutionContext, Future}

import scala.collection.mutable.HashMap

import akka.actor.typed.{ActorRef, ActorSystem}
import soc.akka.messages.StateMessage
import soc.game.inventory.Inventory.{NoInfo, PerfectInfo}

import soc.akka.{GameBehavior, PlayerBehavior, MoveSaverBehavior, MoveResultProvider, RandomMoveResultProvider}
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

import soc.game.{GameConfiguration, GameRules}
import scala.util.{Success, Failure}
//
//object GameContext {
//  implicit val random = new Random()
//
//  val boardConfig = BaseCatanBoard.randomBoard
//
//  val dice = NormalDice()
//
//
//  implicit val gameRules = GameRules()
//
//  val dCardDeck: List[DevelopmentCard] = DevelopmentCardDeckBuilder.buildDeckByCardTypeAndAmount(
//    Map(Knight -> Knight.initAmount,
//      CatanPoint -> CatanPoint.initAmount,
//      RoadBuilder -> RoadBuilder.initAmount,
//      Monopoly -> Monopoly.initAmount,
//      YearOfPlenty -> YearOfPlenty.initAmount)
//  )
//
//  val randSelector = PossibleMoveSelector.randSelector[NoInfo]
//  val players = Map(
//    ("randomPlayer", 0) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player0"),
//    ("randomPlayer", 1) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player1"),
//    ("randomPlayer", 2) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player2"),
//    ("randomPlayer", 3) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player3")
//  )
//
//
//  import CatanResourceSet._
//  import io.circe.generic.auto._
//  //val awsGameSaver = new AWSMoveSaver[BaseBoardConfiguration](CatanGameStoreClientFactory.createClient())
//  //val moveSaverActor: ActorRef[GameMessage] = ActorSystem(MoveSaverBehavior.moveSaverBehavior(awsGameSaver), "moveSaver")
//
//
//  val moveProvider = new RandomMoveResultProvider(dice, dCardDeck)
//  val randomMoveResultProvider = ActorSystem(MoveResultProvider.moveResultProvider(moveProvider), "resultProvider")
//}


