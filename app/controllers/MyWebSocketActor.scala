package controllers

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props}
import chess.api._
import chess.api.actors.{RegisterObserver, UnregisterObserver}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global

object MyWebSocketActor {
  def props(out: ActorRef, chessController: ActorRef) = Props(new MyWebSocketActor(out, chessController))
}

class MyWebSocketActor(val wsOut: ActorRef, val chessController: ActorRef) extends Actor {

  val actionTuple: PartialFunction[Action, Tuple2[String, Position]] = {
    case action: Action => (action.getClass.getSimpleName, action.target)
  }

  chessController ! RegisterObserver

  implicit val timeout: Timeout = Timeout(5, TimeUnit.SECONDS)

  implicit val jsonPositionReads: Reads[Position] = (
    (JsPath \ "x").read[Int] and
      (JsPath \ "y").read[Int]
    ) (Tuple2[Int, Int] _)

  implicit val jsonPositionWrites = new Writes[Position] {
    def writes(position: Position) = Json.obj(
      "x" -> position._1,
      "y" -> position._2
    )
  }

  implicit val actionWrite = new Writes[Action] {
    def writes(action: Action) = Json.obj(
      "name" -> Json.toJson(action.getClass.getSimpleName),
      "target" -> Json.toJson(action.target),
      "choice" -> Json.toJson(action match {
        case a: Choice => a.choice
        case _ => null
      })
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

  implicit val actionsIterableWrites = new Writes[Seq[Action]] {
    override def writes(o: Seq[Action]) = Json.toJson(o)
  }

  implicit val chessBoardWrites = Json.writes[ChessBoard]

  implicit val queryValidActions = Json.format[QueryValidActions]

  val queryMatcher = ReadsMatch[QueryValidActions](queryValidActions)

  implicit val executeAction = Json.format[ExecuteAction]

  val executeMatcher = ReadsMatch[ExecuteAction](executeAction)

  case class ReadsMatch[T](reads: Reads[T]) {
    def unapply(js: JsValue) = reads.reads(js).asOpt
  }

  override def receive = {
    // messages from chess controller
    case update: Update => wsOut ! Json.toJson(update.chessBoard)

    // messages from websocket
    case json: JsValue => json match {
      case queryMatcher(q@QueryValidActions(_)) =>
        chessController ? q onSuccess {
          case actions: Seq[Action] => wsOut ! Json.toJson(actions)
        }
      case executeMatcher(e@ExecuteAction(_,_)) => chessController ! e
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

  trait MessageIn {
    val name: String
  }


}