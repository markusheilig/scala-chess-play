package controllers

import akka.actor.{Actor, ActorRef, Props}
import chess.api._
import play.api.libs.functional.syntax._
import play.api.libs.json._

object MyWebSocketActor {
  def props(out: ActorRef, localController: ActorRef) = Props(new MyWebSocketActor(out, localController))
}

class MyWebSocketActor(val out: ActorRef, val localController: ActorRef) extends Actor {

  // add socket to socket list
  localController ! AppendObserver(self)

  implicit val JsonTuple2: Reads[(Int, Int)] = (
      (JsPath \ "x").read[Int] and
      (JsPath \ "y").read[Int]
    )(Tuple2[Int, Int] _)
  implicit val readsMove = Json.reads[Move]
  implicit val readsCastle = Json.reads[Castle]

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
  }

  def handle[T](jsValue: JsValue)(implicit rds: Reads[T]): Unit = {
    jsValue.validate[T] match {
      case success: JsSuccess[T] => localController ! success.get
      case error: JsError => out ! Json.toJson(JsError.toJson(error))
    }
  }

  override def postStop() = {
    // remove websocket from socket list when socket gets closed
    localController ! RemoveObserver(self)
  }

}