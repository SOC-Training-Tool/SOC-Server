package soc.storage.aws

import soc.aws.client.CatanGameStoreClient
import soc.game.board.BoardConfiguration
import soc.model.PlayerContext
import soc.storage.{MoveEntry, MoveSaver}

import scala.collection.JavaConverters._
import io.circe.syntax._
import io.circe.Encoder
import soc.game.CatanMove._

class AWSMoveSaver[BOARD <: BoardConfiguration](gameStoreClient: CatanGameStoreClient)(implicit moveEncoder: Encoder[List[MoveEntry]], boardEncoder: Encoder[BOARD]) extends MoveSaver[BOARD] {

  var moveMap: Map[Int, List[MoveEntry]] = Map.empty

  override def saveMove(move: MoveEntry): Unit = {
    moveMap = moveMap.get(move.gameId).fold(moveMap + (move.gameId -> List(move))){ ls =>
      (moveMap - move.gameId) + (move.gameId -> (move :: ls))
    }
  }

  override def saveGame(gameId: Int, initBoard: BOARD, players: Map[(String, Int), Int]): Unit = {
    val playerList = players.map { case ((name, id), points) => new PlayerContext(name, id, points)}.toList
    val moves = moveMap(gameId)
    gameStoreClient.save(playerList.asJava, moves.asJson, initBoard.asJson)
    moveMap = moveMap - gameId
  }
}
