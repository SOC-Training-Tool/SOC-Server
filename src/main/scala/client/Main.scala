package client

import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

import soc.protos.game.{CreateGameRequest, CatanServerGrpc}
import soc.protos.game.CatanServerGrpc.CatanServerBlockingStub
import io.grpc.{StatusRuntimeException, ManagedChannelBuilder, ManagedChannel}
import soc.protos.game.StartGameRequest


object CatanGameOrchestrator {
  def apply(host: String, port: Int): CatanGameOrchestrator = {
    val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build
    val blockingStub = CatanServerGrpc.blockingStub(channel)
    new CatanGameOrchestrator(channel, blockingStub)
  }

  def main(args: Array[String]): Unit = {
    val client = CatanGameOrchestrator("localhost", 50051)
    try {
      val target = args.headOption.getOrElse("create")
      if (target == "create") {
          client.createGame()
      } else if (target == "start") {
        val gameId = args.tail.headOption.getOrElse("0")
        client.startGame(gameId)
      } else {
         println("Unrecognized target: " + target)
      }
    } finally {
      client.shutdown()
    }
  }
}

class CatanGameOrchestrator private(
  private val channel: ManagedChannel,
  private val blockingStub: CatanServerBlockingStub
) {
  private[this] val logger = Logger.getLogger(classOf[CatanGameOrchestrator].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def createGame(): Unit = {
    logger.info("Will try to create game")
    val request = CreateGameRequest()
    try {
      val response = blockingStub.createGame(request)
      logger.info("Created game with game id " + response.gameId)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
  }

  def startGame(gameId: String): Unit = {
    logger.info("Will try to start game with id " + gameId + " ...")
    val request = StartGameRequest(gameId = gameId)
    try {
      val response = blockingStub.startGame(request)
      logger.info("Done")
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
  }
}