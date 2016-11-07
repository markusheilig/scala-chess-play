package controllers

import akka.actor.{Actor, ActorRef, Props}
import chess.api._
import chess.api.actors.{RegisterObserver, UnregisterObserver}
import play.api.libs.functional.syntax._
import play.api.libs.json._

object MyWebSocketActor {
  def props(out: ActorRef, chessController: ActorRef) = Props(new MyWebSocketActor(out, chessController))
}

class MyWebSocketActor(val out: ActorRef, val chessController: ActorRef) extends Actor {

  // add socket to socket list
  chessController ! RegisterObserver(self)

  implicit val jsonTuple2Reads: Reads[(Int, Int)] = (
    (JsPath \ "x").read[Int] and
    (JsPath \ "y").read[Int]
  )(Tuple2[Int, Int] _)
  implicit val readsMove = Json.reads[Move]
  implicit val readsCastle = Json.reads[Castle]

  implicit val writesPiece = new Writes[Piece] {
    override def writes(o: Piece) = Json.obj(
      "type" -> o.getClass.getSimpleName,
      "color" -> o.color
    )
  }
  implicit val jsonTuple2Writes = new Writes[(Int, Int)] {
    def writes(tuple: (Int, Int)) = Json.obj(
      "x" -> tuple._1,
      "y" -> tuple._2
    )
  }
  implicit val posPieceWrites = new Writes[((Int, Int), Piece)] {
    def writes(posPiece: ((Int, Int), Piece)) = Json.obj(
      "pos" -> Json.toJson(posPiece._1),
      "piece" -> Json.toJson(posPiece._2)
    )
  }
  implicit val posPieceIterableWrites = new Writes[Iterable[((Int, Int), Piece)]] {
    override def writes(o: Iterable[((Int, Int), Piece)]) = Json.toJson(o)
  }
  implicit val chessBoardWrites = Json.writes[ChessBoard]

  override def receive = {
    case json: JsValue =>
      (json \ "type").validate[String] match {
        case success: JsSuccess[String] => success.get.toLowerCase match {
          case "move" => handle[Move](json)
          case "castle" => handle[Castle](json)
          case other => out ! Json.obj("error" -> s"unknown message type '$other'")
        }
        case error: JsError => out ! Json.obj("error" -> "json property 'type' is missing")
      }
    case update@Update(chessBoard) => {
      println(s"received update: ${update.chessBoard}")
      out ! Json.toJson(update.chessBoard)
    }
  }

  def handle[T](jsValue: JsValue)(implicit rds: Reads[T]): Unit = {
    jsValue.validate[T] match {
      case success: JsSuccess[T] => chessController ! success.get
      case error: JsError => out ! Json.toJson(JsError.toJson(error))
    }
  }

  override def postStop() = {
    // remove websocket from socket list when socket gets closed
    chessController ! UnregisterObserver(self)
  }

}