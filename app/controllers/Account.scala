package controllers

import play.api.mvc._
import models._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import scala.util.Left

object Account extends Controller {

  def login = Action.async { request ⇒
    val map = request.body.asFormUrlEncoded.get
    val username = map("username")(0)
    val password = map("password")(0)
    User.findByName(username) map {
      case Some(user) ⇒
        if(user.pass.left.get.verify(password)) {
          Redirect(routes.Application.index)
            .withSession(request.session + ("authedAs", user.name))
            .flashing("success" -> "Login erfolgreich.")
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

  def profile = Action.async { implicit request ⇒
    RequestContext() map { implicit requestContext ⇒
      if(!requestContext.user.isDefined) {
        Redirect(routes.Application.index).flashing("error" -> "Du musst dich einloggen, bevor du auf dein Profil zugreifen kannst.")
      } else {
        Ok(views.html.Profile())
      }
    }
  }

  def profilePost = Action.async { implicit request ⇒
    RequestContext() flatMap { implicit requestContext ⇒
      if(!requestContext.user.isDefined) {
        Future.successful(
          Redirect(routes.Application.index).flashing("error" -> "Du musst dich einloggen, bevor du auf dein Profil zugreifen kannst.")
        )
      } else {
        val map = request.body.asFormUrlEncoded.get
        map("section")(0) match {
          case "account" ⇒
            requestContext.user.get.copy(
              firstName = map("firstName")(0),
              lastName = map("lastName")(0),
              email = map("email")(0),
              university = map("university")(0)
            ).update map { _ ⇒
              Redirect(routes.Account.profile).flashing("success" -> "Accountinformationen aktualisiert.")
            }
          case "cpw" ⇒
            if(map("pw1")(0) == map("pw2")(0)) {
              requestContext.user.get.copy(pass = Left(PasswordHash.create(map("pw1")(0)))).update map { _ ⇒
                Redirect(routes.Account.profile).flashing("success" -> "Passwort aktualisiert.")
              }
            } else {
              Future.successful(Redirect(routes.Account.profile).flashing("error" -> "Passwörter stimmen nicht überein."))
            }
        }
      }
    }
  }
}
