package controllers

import akka.actor.{Actor, ActorRef}
import chess.api.{Castle, Move}

case class AppendObserver(observer: ActorRef)
case class RemoveObserver(observer: ActorRef)

class LocalController extends Actor {

  val remoteController = context.actorSelection("akka.tcp://chess@127.0.0.1:2552/user/controller")
  val observers = scala.collection.mutable.ListBuffer.empty[ActorRef]

  override def receive: Receive = {
    case AppendObserver(observer) => observers += observer
    case RemoveObserver(observer) => observers -= observer
    case m: Move => remoteController ! m
    case c: Castle => remoteController ! c
    case "START" => remoteController ! "hi from scala-chess-play"
    case msg: String =>
      println(s"received message '$msg'")
      observers.foreach(_ ! s"$msg")
  }
}
