package controllers

import play.api.mvc._
import play.api.Play
import models._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import scala.util.Left
import xyz.wiedenhoeft.scalacrypt._
import play.api.libs.mailer._
import javax.inject.Inject
import scala.util.{ Try, Success, Failure }

class Account @Inject()(mailerClient: MailerClient) extends Controller {

  User.ensureSetup

  def login = Action.async { request ⇒
    val map = request.body.asFormUrlEncoded.get
    val username = map("username")(0)
    val password = map("password")(0)
    User.findByName(username) map {
      case Some(user) ⇒
        if(user.pass.left.get.verify(password)) {
          if(user.activated) {
            Redirect(routes.Application.index)
              .withSession(request.session + ("authedAs", user.name))
              .flashing("success" -> "Login erfolgreich.")
          } else {
            Redirect(routes.Application.index).flashing("error" ->
              "Dein Account ist noch nicht aktiviert. Bitte klicke den Link in deinem Email Postfach."
            )
          }
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

  def register = Action.async { implicit request ⇒
    RequestContext() map { implicit requestContext ⇒
      if(!requestContext.user.isDefined) {
        Ok(views.html.Register())
      } else {
        Redirect(routes.Application.index).flashing("error" -> "Du bist schon eingeloggt.")
      }
    }
  }

  def registerPost = Action.async { implicit request ⇒
    RequestContext() flatMap { implicit requestContext ⇒
      val map = request.body.asFormUrlEncoded.get
      val name = map("name")(0)
      val email = map("email")(0)
      val firstName = map("firstname")(0)
      val lastName = map("lastname")(0)
      val university = map("university")(0)
      val pw1 = map("pw1")(0)
      val pw2 = map("pw2")(0)
      if(pw1 != pw2) {
        Future.successful(Redirect(routes.Account.register).flashing("error" -> "Die Passwörter stimmen nicht überein"))
      } else {
        def bytesToHex(bytes: Seq[Byte]): String = {
          val hexArray = "0123456789ABCDEF".toCharArray
          val hexChars = new Array[Char](bytes.length * 2)
          for(i <- 0 until bytes.length) {
            val v = bytes(i) & 0xFF
            hexChars(i * 2) = hexArray(v >>> 4)
            hexChars(i * 2 + 1) = hexArray(v & 0x0F)
          }
          return new String(hexChars);
        }

        val user = User(
          name,
          Left(PasswordHash.create(pw1)),
          firstName,
          lastName,
          email,
          university,
          false,
          bytesToHex(Random.nextBytes(32))
        )

        User.register(user) map {
          case Success(user) ⇒
            val conf = Play.current.configuration
            val baseUri = conf.getString("app.baseUri").get
            val link = baseUri + "/activate/" + user.name + "/" + user.activationSecret
            val username = user.name
            val email = Email(
              "Aktivierung ZaPF-Account",
              conf.getString("app.email").get,
              Seq(user.email),
              bodyText = Some(
                s"""
                  |Hi,
                  |
                  |Um deinen Account $username zu aktivieren, musst du den folgenden Link klicken:
                  |
                  |$link
                  |
                  |Grüße,
                  |Der ToPF
                """.stripMargin
              )
            )
            mailerClient.send(email)
            Redirect(routes.Application.index).flashing("notice" ->
              "Dir wurde ein Aktivierungslink per Email geschickt. Bitte besuche diesen Link um deinen Account zu bestätigen."
            )
          case Failure(f) ⇒
            Redirect(routes.Account.register).flashing("error" -> f.getMessage)
        }
      }
    }
  }

  def activate(user: String, key: String) = Action.async { implicit request ⇒
    RequestContext() flatMap { implicit requestContext ⇒
      User.findByName(user) flatMap {
        case Some(user) ⇒
          if(user.activationSecret == key) {
            user.copy(activated = true).update map {
              case Success(user) ⇒ Redirect(routes.Application.index).flashing("success" -> "Aktivierung erfolgreich.")
              case Failure(f) ⇒ Redirect(routes.Application.index).flashing("error" -> f.getMessage)
            }
          } else {
            Future.successful(Redirect(routes.Application.index).flashing("error" -> "Ungültige Aktivierungsparameter"))
          }
        case None ⇒
          Future.successful(Redirect(routes.Application.index).flashing("error" -> "Ungültige Aktivierungsparameter"))
      }
    }
  }
}
