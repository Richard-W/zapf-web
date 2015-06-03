package controllers

import play.api.mvc._
import play.api.Play
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.mailer._

import scala.concurrent.Future
import scala.util.Left
import scala.util.{ Try, Success, Failure }
import javax.inject.Inject
import xyz.wiedenhoeft.scalacrypt._

import models._

class Account @Inject()(mailerClient: MailerClient) extends ExtendedController {

  User.ensureSetup

  def login = Action.async { request ⇒
    val map = request.body.asFormUrlEncoded.get
    val username = map("username")(0)
    val password = map("password")(0)
    User.findByName(username) map {
      case Some(user) ⇒
        if(user.pass.left.get.verify(password)) {
          if(user.activated) {
            success(routes.Application.index, "Login erfolgreich.").withSession(request.session + ("authedAs", user.name))
          } else {
            error(routes.Application.index, "Dein Account ist noch nicht aktiviert. Bitte klicke den Link in deinem Email Postfach.")
          }
        } else {
          error(routes.Application.index, "Nutzername / Passwort inkorrekt")
        }
      case None ⇒
        error(routes.Application.index, "Nutzername / Passwort inkorrekt")
    }
  }

  def logout = Action { implicit request ⇒
    Redirect(routes.Application.index).withSession(request.session - "authedAs")
  }

  def profile = contextAction1 { implicit requestContext ⇒
    if(!requestContext.user.isDefined) {
      error(routes.Application.index, "Du musst dich einloggen, bevor du auf dein Profil zugreifen kannst.")
    } else {
      Ok(views.html.Profile())
    }
  }

  def profilePost = asyncContextAction2 { implicit request ⇒ implicit requestContext ⇒
    if(!requestContext.user.isDefined) {
      Future.successful(
        error(routes.Application.index, "Du musst dich einloggen, bevor du auf dein Profil zugreifen kannst.")
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
            success(routes.Account.profile, "Accountinformationen aktualisiert.")
          }
        case "cpw" ⇒
          if(map("pw1")(0) == map("pw2")(0)) {
            requestContext.user.get.copy(pass = Left(PasswordHash.create(map("pw1")(0)))).update map { _ ⇒
              success(routes.Account.profile, "Passwort aktualisiert.")
            }
          } else {
            Future.successful(error(routes.Account.profile, "Passwörter stimmen nicht überein."))
          }
      }
    }
  }

  def register = contextAction1 { implicit requestContext ⇒
    if(!requestContext.user.isDefined) {
      Ok(views.html.Register())
    } else {
      error(routes.Application.index, "Du bist schon eingeloggt.")
    }
  }

  def registerPost = asyncContextAction2 { implicit request ⇒ implicit requestContext ⇒
    val map = request.body.asFormUrlEncoded.get
    val name = map("name")(0)
    val email = map("email")(0)
    val firstName = map("firstname")(0)
    val lastName = map("lastname")(0)
    val university = map("university")(0)
    val pw1 = map("pw1")(0)
    val pw2 = map("pw2")(0)
    if(pw1 != pw2) {
      Future.successful(error(routes.Account.register, "Die Passwörter stimmen nicht überein"))
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
          notice(routes.Application.index, "Dir wurde ein Aktivierungslink per Email geschickt. Bitte besuche diesen Link um deinen Account zu bestätigen.")
        case Failure(f) ⇒
          error(routes.Account.register, f.getMessage)
      }
    }
  }

  def activate(user: String, key: String) = asyncContextAction2 { implicit request ⇒ implicit requestContext ⇒
    User.findByName(user) flatMap {
      case Some(user) ⇒
        if(user.activationSecret == key) {
          user.copy(activated = true).update map {
            case Success(user) ⇒ success(routes.Application.index, "Aktivierung erfolgreich.")
            case Failure(f) ⇒ error(routes.Application.index, f.getMessage)
          }
        } else {
          Future.successful(error(routes.Application.index, "Ungültige Aktivierungsparameter"))
        }
      case None ⇒
        Future.successful(error(routes.Application.index, "Ungültige Aktivierungsparameter"))
    }
  }
}
