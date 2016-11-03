package controllers

import akka.actor.{Actor, ActorRef, Props}
import chess.api.MoveAndChangeChoice.MoveAndChangeChoice
import chess.api._
import play.api.libs.functional.syntax._
import play.api.libs.json._

object MyWebSocketActor {
  def props(out: ActorRef) = Props(new MyWebSocketActor(out))
}

class MyWebSocketActor(out: ActorRef) extends Actor {

  implicit val JsonTuple2: Reads[(Int, Int)] = (
    (JsPath \ "x").read[Int] and
      (JsPath \ "y").read[Int]
    )(Tuple2[Int, Int] _)
  implicit val readsMove = Json.reads[Move]
  implicit val readsCastle = Json.reads[Castle]

  def receive = {
    case json: JsValue => {
      (json \ "type").validate[String] match {
        case success: JsSuccess[String] => success.get.toLowerCase match {
          case "move" => handle[Move](json)
          case "castle" => handle[Castle](json)
          case _ => out ! Json.obj("error" -> "unknown message type")
        }
        case error: JsError => out ! Json.obj("error" -> "invalid message format")
      }
    }
  }

  def handle[T](jsValue: JsValue)(implicit rds: Reads[T]): Unit = {
    jsValue.validate[T] match {
      case success: JsSuccess[T] => {
        println(s"valid message format for ${success.get.getClass.getSimpleName}")
      }
      case error: JsError => println(error.toString)
    }
  }

}