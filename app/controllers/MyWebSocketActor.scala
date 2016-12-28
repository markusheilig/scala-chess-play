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

  implicit val colorWrites = new Writes[Color.Value] {
    override def writes(o: _root_.chess.api.Color.Value): JsValue = JsString(o.toString)
  }
  implicit val colorReads = Reads.enumNameReads(Color)
  implicit val pieceFormat = Json.format[Piece]
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

  implicit val readsPut = Json.format[Put]
  val put = ReadsMatch[Put]((JsPath \ "put").read[Put])

  implicit val readsRemove = Json.format[Remove]
  val remove = ReadsMatch[Remove]((JsPath \ "remove").read[Remove])

  implicit val readsMove = Json.format[Move]
  val move = ReadsMatch[Move]((JsPath \ "move").read[Move])

  implicit val readsCastle = Json.format[Castle]
  val castle = ReadsMatch[Castle]((JsPath \ "castle").read[Castle])

  implicit val readsPutInitial = Json.format[PutInitial]
  implicit val readsReplace = Json.format[Replace]
  implicit val readsMoveAndReplace = Json.format[MoveAndReplace]
  val moveAndReplace = ReadsMatch[MoveAndReplace]((JsPath \ "moveandreplace").read[MoveAndReplace])

  override def receive = {
    // messages from chess controller
    case update: Update => wsOut ! Json.toJson(update.chessBoard)

    // messages from websocket
    case json: JsValue => json match {
      case castle(c@Castle(_,_,_)) => chessController ! c
      case move(m@Move(_,_,_,_)) => chessController ! m
      case put(p@Put(_, _)) => chessController ! p
      case remove(r@Remove(_, _)) => chessController ! r
      case moveAndReplace(mar@MoveAndReplace(_, _, _, _)) => chessController ! mar
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