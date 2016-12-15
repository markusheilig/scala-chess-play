package controllers

import akka.actor.{Actor, ActorRef, Props}
import chess.api.{Piece, _}
import chess.api.actors.{RegisterObserver, UnregisterObserver}
import play.api.libs.functional.syntax._
import play.api.libs.json._


object MyWebSocketActor {
  def props(out: ActorRef, chessController: ActorRef) = Props(new MyWebSocketActor(out, chessController))


}

class MyWebSocketActor(val out: ActorRef, val chessController: ActorRef) extends Actor {

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

  implicit val readsRemove = Json.reads[Remove]
  implicit val readsPut = Json.reads[Put]
  implicit val readsMove = Json.reads[Move]
  implicit val readsCastle = Json.reads[Castle]


  implicit val pieceWrites = new Writes[Piece] {
    override def writes(p: Piece): JsValue = Json.obj(
      "id" -> p.id,
      "color" -> p.color.toString,
      "type" -> p.getClass.getSimpleName
    )
  }

  implicit val formatPositionPiece = Json.format[(Position, Piece)]
  //implicit val formatSeqPositionPiece = Json.format[Seq[(Position, Piece)]]

  implicit val chessBoardWrites = Json.writes[ChessBoard]

  implicit val posPieceWrites = new Writes[(Position, Piece)] {
    def writes(posPiece: (Position, Piece)) = Json.obj(
      "pos" -> Json.toJson(posPiece._1),
      "piece" -> Json.toJson(posPiece._2)
    )
  }
  implicit val posPieceIterableWrites = new Writes[Iterable[(Position, Piece)]] {
    override def writes(o: Iterable[(Position, Piece)]) = Json.toJson(o)
  }

  implicit val improvedNameReads =
    (JsPath \ "undo").read[Undo]

  case class ReadsMatch[T](reads: Reads[T]) {
    def unapply(js: JsValue) = reads.reads(js).asOpt
  }

  val move = ReadsMatch[Move](Json.reads[Move])
  val castle = ReadsMatch[Castle](Json.reads[Castle])
  val undo = ReadsMatch[Undo](Json.reads[Undo])

  override def receive = {
    case json: JsValue => json match {
        case move(m@Move(_,_,_,_)) => chessController ! m
        case castle(c@Castle(_,_,_)) => chessController ! c
        case undo(u@Undo()) => chessController ! u
      }
  }

      /*
      (json \ "type").validate[String] match {
        case success: JsSuccess[String] => success.get.toLowerCase match {
          //case "move" => handle[Move](json)
          //case "castle" => handle[Castle](json)
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
    // unregister observer when socket gets closed
    chessController ! UnregisterObserver
  }

  trait MyAction
  case class MyMove(x: Int) extends MyAction
  case class AndreasMove(q: String) extends MyAction

  val myMoveReadsMatch = ReadsMatch[MyMove](Json.reads[MyMove])
  val andreasMoveReadsMatch = ReadsMatch[AndreasMove](Json.reads[AndreasMove])

  def dohandle(x: JsValue) = x match {
    case myMoveReadsMatch(a@MyMove(8)) => 199999
    case myMoveReadsMatch(a@MyMove(_)) => a.x
    case andreasMoveReadsMatch(b@AndreasMove(_)) => b.q
    case _ => -1
  }
  */
}