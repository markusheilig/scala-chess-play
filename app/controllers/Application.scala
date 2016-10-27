package controllers

import javax.inject.Inject

import play.api._
import play.api.mvc._
import akka.actor._
import akka.stream.Materializer
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow

class Application @Inject() (implicit system: ActorSystem, materializer: Materializer) extends Controller {

  def socket = WebSocket.accept[JsValue, JsValue] { request =>
    ActorFlow.actorRef(out => MyWebSocketActor.props(out))
  }

  def index = Action {
    Ok(views.html.main())
  }

}