package controllers

import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok(views.html.Index())
  }

  def aboutAllgemeines = Action {
    Ok(views.html.about.Allgemeines())
  }

  def aboutFachschaften = Action {
    Ok(views.html.about.Fachschaften())
  }

  def aboutGeschichte = Action {
    Ok(views.html.about.Geschichte())
  }

  def stapf = Action {
    Ok(views.html.Stapf())
  }

  def beschluesse = Action {
    Ok(views.html.Beschluesse())
  }
}
