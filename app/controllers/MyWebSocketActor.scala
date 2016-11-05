package controllers

import akka.actor.{Actor, ActorRef, Props}
import chess.api._
import play.api.libs.functional.syntax._
import play.api.libs.json._

object MyWebSocketActor {
  def props(out: ActorRef, localController: ActorRef) = {
    localController ! AppendSocket(out)
    Props(new MyWebSocketActor(out, localController))
  }
}

class MyWebSocketActor(val out: ActorRef, val localController: ActorRef) extends Actor {

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
        case error: JsError => out ! Json.obj("error" -> "property 'type' is missing")
      }
    case _ => out ! Json.obj("error" -> "invaliad json format")
  }

  def handle[T](jsValue: JsValue)(implicit rds: Reads[T]): Unit = {
    jsValue.validate[T] match {
      case success: JsSuccess[T] => localController ! success.get
      case error: JsError => println(error.toString)
    }
  }

  override def postStop(): Unit = {
    localController ! RemoveSocket(self)
  }

}