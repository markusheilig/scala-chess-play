package controllers

import javax.inject.Inject

import akka.actor._
import akka.stream.Materializer
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc._

class LocalController extends Actor {

  val remoteController = context.actorSelection("akka.tcp://chess@127.0.0.1:2552/user/controller")

  override def receive: Receive = {
    case "START" => remoteController ! "hi from scala-chess-play"
    case msg: String => println(s"received message '$msg'")
  }
}

class Application @Inject() (implicit system: ActorSystem, materializer: Materializer) extends Controller {

  val localController = system.actorOf(Props[LocalController], name = "remoteController")
  localController ! "START"

  def socket = WebSocket.accept[JsValue, JsValue] { request =>
    ActorFlow.actorRef(out => MyWebSocketActor.props(out))
  }

  def index = Action {
    Ok(views.html.main())
  }

}