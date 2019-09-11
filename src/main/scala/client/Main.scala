package client

import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

import soc.protos.game.{CreateGameRequest, CatanServerGrpc}
import soc.protos.game.CatanServerGrpc.CatanServerBlockingStub
import io.grpc.{StatusRuntimeException, ManagedChannelBuilder, ManagedChannel}
import soc.protos.game.StartGameRequest

/**
 * [[https://github.com/grpc/grpc-java/blob/v0.15.0/examples/src/main/java/io/grpc/examples/helloworld/HelloWorldClient.java]]
 */
object HelloWorldClient {
  def apply(host: String, port: Int): HelloWorldClient = {
    val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build
    val blockingStub = CatanServerGrpc.blockingStub(channel)
    new HelloWorldClient(channel, blockingStub)
  }

  def main(args: Array[String]): Unit = {
    val client = HelloWorldClient("localhost", 50051)
    try {
      val target = args.headOption.getOrElse("world")
      client.startGame("foo")
    } finally {
      client.shutdown()
    }
  }
}

class HelloWorldClient private(
  private val channel: ManagedChannel,
  private val blockingStub: CatanServerBlockingStub
) {
  private[this] val logger = Logger.getLogger(classOf[HelloWorldClient].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def createGame(): Unit = {
    logger.info("Will try to create game")
    val request = CreateGameRequest()
    try {
      val response = blockingStub.createGame(request)
      logger.info("Greeting: " + response.gameId)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
  }

  def startGame(name: String): Unit = {
    logger.info("Will try to start Game " + name + " ...")
    val request = StartGameRequest()
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