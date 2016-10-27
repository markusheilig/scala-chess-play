package controllers

import akka.actor.{Actor, ActorRef, Props}
import play.api.libs.functional.syntax._
import play.api.libs.json._

object ChessApiReads {
  import chess.api.{Castle, Move}
  implicit val JsonTuple2: Reads[(Int, Int)] = (
    (JsPath \ "x").read[Int] and
      (JsPath \ "y").read[Int]
    )(Tuple2[Int, Int] _)
  implicit val JsonMove = Json.reads[Move]
  implicit val JsonCastle = Json.reads[Castle]
}

object MyWebSocketActor {
  def props(out: ActorRef) = Props(new MyWebSocketActor(out))
}

class MyWebSocketActor(out: ActorRef) extends Actor {
  def receive = {
    case q: JsValue => out ! q
  }
}