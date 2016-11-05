package controllers

import akka.actor.{Actor, ActorRef}

case class AppendSocket(actorRef: ActorRef)
case class RemoveSocket(actorRef: ActorRef)

class LocalController extends Actor {

  val remoteController = context.actorSelection("akka.tcp://chess@127.0.0.1:2552/user/controller")
  val outSockets = scala.collection.mutable.ListBuffer.empty[ActorRef]

  override def receive: Receive = {
    case AppendSocket(socket) => outSockets += socket
    case RemoveSocket(socket) => outSockets -= socket
    case "START" => remoteController ! "hi from scala-chess-play"
    case msg: String =>
      println(s"received message '$msg'")
      outSockets.foreach(_ ! s"$msg")
  }
}
