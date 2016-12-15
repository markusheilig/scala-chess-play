package controllers

import akka.actor.{Actor, ActorRef, Props}
import chess.api._
import chess.api.actors.{RegisterObserver, UnregisterObserver}
import play.api.libs.functional.syntax._
import play.api.libs.json._

object MyWebSocketActor {
  def props(out: ActorRef, chessController: ActorRef) = Props(new MyWebSocketActor(out, chessController))
}

class MyWebSocketActor(val wsOut: ActorRef, val chessController: ActorRef) extends Actor {

  chessController ! RegisterObserver

  implicit val jsonPositionReads: Reads[Position] = (
    (JsPath \ "x").read[Int] and
      (JsPath \ "y").read[Int]
    )(Tuple2[Int, Int] _)
  implicit val jsonPositionWrites = new Writes[Position] {
    def writes(position: Position) = Json.obj(
      "x" -> position._1,
      "y" -> position._2
    )
  }

  implicit val readsRemove = Json.format[Remove]
  implicit val readsPut = Json.format[Put]
  implicit val readsMove = Json.format[Move]
  implicit val readsCastle = Json.format[Castle]

  implicit val pieceWrites = new Writes[Piece] {
    override def writes(p: Piece): JsValue = Json.obj(
      "id" -> p.id,
      "color" -> p.color.toString,
      "type" -> p.getClass.getSimpleName
    )
  }
  implicit val posPieceWrites = new Writes[(Position, Piece)] {
    def writes(posPiece: (Position, Piece)) = Json.obj(
      "pos" -> Json.toJson(posPiece._1),
      "piece" -> Json.toJson(posPiece._2)
    )
  }
  implicit val posPieceIterableWrites = new Writes[Seq[(Position, Piece)]] {
    override def writes(o: Seq[(Position, Piece)]) = Json.toJson(o)
  }

  implicit val chessBoardWrites = Json.writes[ChessBoard]

  case class ReadsMatch[T](reads: Reads[T]) {
    def unapply(js: JsValue) = reads.reads(js).asOpt
  }

  val castle = ReadsMatch[Castle](Json.reads[Castle])
  val move = ReadsMatch[Move](Json.reads[Move])
  val put = ReadsMatch[Put](Json.reads[Put])
  val remove = ReadsMatch[Remove](Json.reads[Remove])

  override def receive = {
    // messages from chess controller
    case update: Update => wsOut ! Json.toJson(update.chessBoard)

    // messages from websocket
    case json: JsValue => json match {
      case castle(c@Castle(_,_,_)) => chessController ! c
      case move(m@Move(_,_,_,_)) => chessController ! m
      case put(p@Put(_, _)) => chessController ! p
      case remove(r@Remove(_, _)) => chessController ! r
      case unknownMessage@_ => {
        println(s"unknown message: $unknownMessage")
        wsOut ! Json.toJson(unknownMessage)
      }
    }
  }

  override def postStop() = {
    // unregister observer when socket gets closed
    chessController ! UnregisterObserver
  }

}