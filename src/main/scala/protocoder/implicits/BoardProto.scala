package protocoder.implicits

import soc.board._
import soc.core.Roll
import soc.inventory._
import protocoder.ProtoCoder
import protocoder.ProtoCoder.ops._
import protos.soc.board.{Board => PBoard, Hex => PHex, Port => PPort, Vertex => PVertex, Edge => PEdge, VertexBuilding => PVertexBuilding, EdgeBuilding => PEdgeBuilding}

object BoardProto {

  implicit def protoBoard[T <: BoardConfiguration](implicit boardMapping: BoardMapping[T]): ProtoCoder[CatanBoard, PBoard] = { board =>
    val hexes = board.hexesWithNodes.map(_.proto)
    val ports = board.portMap.toSeq.map(_.proto)
    val robberHex = hexes.find(_.id == boardMapping.hexMapping(board.robberHex)).get
    val vertexBuildings = board.verticesBuildingMap.toSeq.map { _.proto }
    val edgeBuildings = board.edgesBuildingMap.toSeq.map { _.proto }
    PBoard(hexes, ports, robberHex, vertexBuildings, edgeBuildings)
  }

  implicit def boardFromProto[T <: BoardConfiguration](implicit boardMapping: BoardMapping[T]): ProtoCoder[PBoard, CatanBoard] = { protoBoard =>
    val hexes = protoBoard.hexes.map(_.proto)
    val portMap = protoBoard.ports.map(_.proto).toMap
    val robberHex = protoBoard.robberHex.proto.node
    val vertexBuildings = protoBoard.vertexBuildings.map(_.proto).toMap
    val edgeBuildings = protoBoard.edgeBuildings.map(_.proto).toMap
    CatanBoard(hexes, portMap, robberHex, vertexBuildings, edgeBuildings)
  }

  implicit def protoHex[T <: BoardConfiguration](implicit boardMapping: BoardMapping[T]): ProtoCoder[BoardHex, PHex] = { boardHex =>
    val id = boardMapping.hexMapping(boardHex.node)
    val adjacentVertices = boardHex.vertices.map(_.proto)
    boardHex.hex.getResourceAndNumber.fold(PHex(id, PHex.Resource.DESERT, None, adjacentVertices)) { case (res, roll) =>
      val protoRes = res match {
        case Brick => PHex.Resource.BRICK
        case Ore => PHex.Resource.ORE
        case Sheep => PHex.Resource.SHEEP
        case Wheat => PHex.Resource.WHEAT
        case Wood => PHex.Resource.WOOD
      }
      PHex(id, protoRes, Some(PHex.HexProbability(roll.number, roll.dots)), adjacentVertices)
    }
  }

  implicit def hexFromProto[T <: BoardConfiguration](implicit boardMapping: BoardMapping[T]): ProtoCoder[PHex, BoardHex] = { protoHex =>
    val node = boardMapping.reverseHexMapping(protoHex.id)
    val roll = protoHex.hexProbability.map(n => Roll(n.number))
    val hex = protoHex.resource match {
      case PHex.Resource.BRICK => ResourceHex(Brick, roll.get)
      case PHex.Resource.ORE => ResourceHex(Ore, roll.get)
      case PHex.Resource.SHEEP => ResourceHex(Sheep, roll.get)
      case PHex.Resource.WHEAT => ResourceHex(Wheat, roll.get)
      case PHex.Resource.WOOD => ResourceHex(Wood, roll.get)
      case _ => Desert
    }
    val adjacentVertices = protoHex.adjacentVertices.map(_.proto).toList
    BoardHex(node, hex, adjacentVertices)
  }

  implicit def protoPort[T <: BoardConfiguration](implicit boardMapping: BoardMapping[T]): ProtoCoder[(Edge, Port), PPort] = { p =>
    val (edge, port) = p
    val portType = port match {
      case Brick => PPort.PortType.BRICK
      case Ore => PPort.PortType.ORE
      case Sheep => PPort.PortType.SHEEP
      case Wheat => PPort.PortType.WHEAT
      case Wood => PPort.PortType.WOOD
      case Misc => PPort.PortType.MISC
    }
    PPort(portType, edge.proto)
  }

  implicit def portFromProto[T <: BoardConfiguration](implicit boardMapping: BoardMapping[T]): ProtoCoder[PPort, (Edge, Port)] = { protoPort =>
    val portType = protoPort.portType match {
      case PPort.PortType.BRICK => Brick
      case PPort.PortType.ORE => Ore
      case PPort.PortType.SHEEP => Sheep
      case PPort.PortType.WHEAT => Wheat
      case PPort.PortType.WOOD => Wood
      case PPort.PortType.MISC => Misc
    }
    (protoPort.edge.proto, portType)
  }

  implicit def protoVertex[T <: BoardConfiguration](implicit boardMapping: BoardMapping[T]): ProtoCoder[Vertex, PVertex] = vertex => PVertex(boardMapping.vertexMapping(vertex.node))
  implicit def vertexFromProto[T <: BoardConfiguration](implicit boardMapping: BoardMapping[T]): ProtoCoder[PVertex, Vertex] = protoVertex => Vertex(boardMapping.reverseVertexMapping(protoVertex.id))
  implicit def protoEdge[T <: BoardConfiguration](implicit boardMapping: BoardMapping[T]): ProtoCoder[Edge, PEdge] = edge => PEdge(edge.v1.proto, edge.v2.proto)
  implicit def edgeFromProto[T <: BoardConfiguration](implicit boardMapping: BoardMapping[T]): ProtoCoder[PEdge, Edge] = protoEdge => Edge(protoEdge.v1.proto, protoEdge.v2.proto)

  implicit def protoVertexBuilding[T <: BoardConfiguration](implicit boardMapping: BoardMapping[T]): ProtoCoder[(Vertex, VertexBuilding), PVertexBuilding] = { vVertexBuilding =>
    val (vertex, vertexBuilding) = vVertexBuilding
    vertexBuilding match {
      case Settlement(p) => PVertexBuilding(PVertexBuilding.BuildingType.SETTLEMENT, vertex.proto, p)
      case City(p) => PVertexBuilding(PVertexBuilding.BuildingType.CITY, vertex.proto, p)
    }
  }

  implicit def vertexBuildingFromProto[T <: BoardConfiguration](implicit boardMapping: BoardMapping[T]): ProtoCoder[PVertexBuilding, (Vertex, VertexBuilding)] = { protoVertexBuilding =>
    protoVertexBuilding.buildingType match {
      case PVertexBuilding.BuildingType.SETTLEMENT => (protoVertexBuilding.vertex.proto, Settlement(protoVertexBuilding.playerPosition))
      case PVertexBuilding.BuildingType.CITY => (protoVertexBuilding.vertex.proto, City(protoVertexBuilding.playerPosition))
    }
  }

  implicit def protoEdgeBuilding[T <: BoardConfiguration](implicit boardMapping: BoardMapping[T]): ProtoCoder[(Edge, EdgeBuilding), PEdgeBuilding] = { eEdgeBuilding =>
    val (edge, edgeBuilding) = eEdgeBuilding
    edgeBuilding match {
      case Road(p) => PEdgeBuilding(PEdgeBuilding.BuildingType.ROAD, edge.proto, p)
    }
  }

  implicit def edgeBuildingFromProto[T <: BoardConfiguration](implicit boardMapping: BoardMapping[T]): ProtoCoder[PEdgeBuilding, (Edge, EdgeBuilding)] = { protoEdgeBuilding =>
    protoEdgeBuilding.buildingType match {
      case PEdgeBuilding.BuildingType.ROAD => (protoEdgeBuilding.edge.proto, Road(protoEdgeBuilding.playerPosition))
    }
  }
}
