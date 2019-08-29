package soc.akka.messages

import soc.game.moves.CatanMove

case class Response(playerId: Int, move: CatanMove) extends PlayerMessage