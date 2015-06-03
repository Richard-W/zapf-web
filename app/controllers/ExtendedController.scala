package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

import models._

class ExtendedController extends Controller {

  def contextAction2(block: (Request[AnyContent]) ⇒ (RequestContext) ⇒ Result): Action[AnyContent] = {
    Action.async { implicit request ⇒
      RequestContext() map { requestContext ⇒
        block(request)(requestContext)
      }
    }
  }

  def contextAction1(block: (RequestContext) ⇒ Result): Action[AnyContent] = {
    Action.async { implicit request ⇒
      RequestContext() map { requestContext ⇒
        block(requestContext)
      }
    }
  }

  def asyncContextAction2(block: (Request[AnyContent]) ⇒ (RequestContext) ⇒ Future[Result]): Action[AnyContent] = {
    Action.async { implicit request ⇒
      RequestContext() flatMap { requestContext ⇒
        block(request)(requestContext)
      }
    }
  }

  def asyncContextAction1(block: (RequestContext) ⇒ Future[Result]): Action[AnyContent] = {
    Action.async { implicit request ⇒
      RequestContext() flatMap { requestContext ⇒
        block(requestContext)
      }
    }
  }

  def success(redirect: Call, message: String) = {
    Redirect(redirect).flashing("success" -> message)
  }

  def error(redirect: Call, message: String) = {
    Redirect(redirect).flashing("error" -> message)
  }

  def warning(redirect: Call, message: String) = {
    Redirect(redirect).flashing("warning" -> message)
  }

  def notice(redirect: Call, message: String) = {
    Redirect(redirect).flashing("notice" -> message)
  }
}
