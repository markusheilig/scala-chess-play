package controllers

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import akka.actor._
import akka.stream.Materializer
import play.api.libs.json.{JsValue, Json, Reads}
import play.api.libs.streams.ActorFlow
import play.api.mvc._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class Application @Inject() (implicit system: ActorSystem, materializer: Materializer) extends Controller {

  def socket = WebSocket.accept[JsValue, JsValue] { request =>
    val actorPath = chess.api.actors.Config.controllerActorPath
    val selection = system.actorSelection(actorPath)
    implicit val timeout = akka.util.Timeout(5, TimeUnit.SECONDS)
    val controller = Await.result(selection.resolveOne(), Duration.Inf)
    ActorFlow.actorRef(out => MyWebSocketActor.props(out, controller))
  }

  def index = Action {
    Ok(views.html.main())
  }
}