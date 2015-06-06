package models

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.modules.authenticator._

case class RequestContext(
  principal: Option[Principal],
  success: Option[String],
  error: Option[String],
  warning: Option[String],
  notice: Option[String]
)

object RequestContext {

  def apply()(implicit auth: Authenticator, request: Request[AnyContent]): Future[RequestContext] = {
    auth.principal map { principal ⇒
      RequestContext(
        principal,
        request.flash.get("success"),
        request.flash.get("error"),
        request.flash.get("warning"),
        request.flash.get("notice")
      )
    }
  }
}
