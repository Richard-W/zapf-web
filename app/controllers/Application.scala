package controllers

import play.api.mvc._
import models._
import play.api.libs.concurrent.Execution.Implicits._

object Application extends Controller {

  User.ensureSetup

  def index = Action.async { implicit request ⇒
    RequestContext() map { implicit requestContext ⇒
      Ok(views.html.Index())
    }
  }

  def login = Action.async { request ⇒
    val map = request.body.asFormUrlEncoded.get
    val username = map("username")(0)
    val password = map("password")(0)
    User.findByName(username) map {
      case Some(user) ⇒
        if(user.pass.left.get.verify(password)) {
          Redirect(routes.Application.index).withSession(request.session + ("authedAs", user.name))
        } else {
          Redirect(routes.Application.index).flashing("error" -> "Nutzername / Passwort inkorrekt")
        }
      case None ⇒
        Redirect(routes.Application.index).flashing("error" -> "Nutzername / Passwort inkorrekt")
    }
  }

  def logout = Action { implicit request ⇒
    Redirect(routes.Application.index).withSession(request.session - "authedAs")
  }

  def profile = TODO

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
