package soc.game.messages

import soc.game.CatanMove

case class Response(playerId: Int, move: CatanMove) extends PlayerMessage