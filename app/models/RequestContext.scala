package models

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Request

case class RequestContext(
  user: Option[User],
  success: Option[String],
  error: Option[String],
  warning: Option[String],
  notice: Option[String]
)

object RequestContext {

  def apply()(implicit request: Request[Any]): Future[RequestContext] = {
    val futureUser: Future[Option[User]] = request.session.get("authedAs") match {
      case Some(username) ⇒ User.findByName(username)
      case None ⇒ Future.successful(None)
    }

    futureUser map { user ⇒
      RequestContext(
        user,
        request.flash.get("success"),
        request.flash.get("error"),
        request.flash.get("warning"),
        request.flash.get("notice")
      )
    }
  }
}
