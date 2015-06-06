package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import models._
import play.modules.authenticator._
import javax.inject._

class Application @Inject()(implicit val auth: Authenticator) extends ExtendedController {

  def index = contextAction1 { implicit requestContext ⇒
    Ok(views.html.Index())
  }

  def aboutAllgemeines = contextAction1 { implicit requestContext ⇒
    Ok(views.html.about.Allgemeines())
  }

  def aboutFachschaften = contextAction1 { implicit requestContext ⇒
    Ok(views.html.about.Fachschaften())
  }

  def aboutGeschichte = contextAction1 { implicit requestContext ⇒
    Ok(views.html.about.Geschichte())
  }

  def stapf = contextAction1 { implicit requestContext ⇒
    Ok(views.html.Stapf())
  }

  def beschluesse = contextAction1 { implicit requestContext ⇒
    Ok(views.html.Beschluesse())
  }
}
