package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import models._

object Application extends Controller {

  def index = Action.async { implicit request ⇒
    RequestContext() map { implicit requestContext ⇒
      Ok(views.html.Index())
    }
  }

  def aboutAllgemeines = Action.async { implicit request ⇒
    RequestContext() map { implicit requestContext ⇒
      Ok(views.html.about.Allgemeines())
    }
  }

  def aboutFachschaften = Action.async { implicit request ⇒
    RequestContext() map { implicit requestContext ⇒
      Ok(views.html.about.Fachschaften())
    }
  }

  def aboutGeschichte = Action.async { implicit request ⇒
    RequestContext() map { implicit requestContext ⇒
      Ok(views.html.about.Geschichte())
    }
  }

  def stapf = Action.async { implicit request ⇒
    RequestContext() map { implicit requestContext ⇒
      Ok(views.html.Stapf())
    }
  }

  def beschluesse = Action.async { implicit request ⇒
    RequestContext() map { implicit requestContext ⇒
      Ok(views.html.Beschluesse())
    }
  }
}
