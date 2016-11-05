package controllers

import javax.inject.Inject

import akka.actor._
import akka.stream.Materializer
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc._

class Application @Inject() (implicit system: ActorSystem, materializer: Materializer) extends Controller {

  val localController = system.actorOf(Props[LocalController], "localController")
  localController ! "START"

  def socket = WebSocket.accept[JsValue, JsValue] { request =>
    ActorFlow.actorRef(out => MyWebSocketActor.props(out, localController))
  }

  def index = Action {
    Ok(views.html.main())
  }

}