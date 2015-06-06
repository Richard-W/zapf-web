package controllers

import play.api._
import play.api.mvc._
import play.api.Play
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.mailer._

import scala.concurrent.Future
import scala.util.Left
import scala.util.{ Try, Success, Failure }
import javax.inject.Inject
import xyz.wiedenhoeft.scalacrypt._
import play.modules.authenticator._

import models._

class Account @Inject()(mailerClient: MailerClient, implicit val auth: Authenticator, conf: Configuration) extends ExtendedController {

  // Insert admin if it does not exist
  conf.getString("admin.initpass") match {
    case Some(pass) ⇒ auth.principals.create("admin", pass) map {
      case Success(princ) ⇒ princ.flag("activated", true).save
      case Failure(f) ⇒ throw f
    }
    case None ⇒
  }

  def login = Action.async { implicit request ⇒
    val map = request.body.asFormUrlEncoded.get
    val username = map("username")(0)
    val password = map("password")(0)
    auth.authenticate(username, password) {
      case Some(princ) ⇒
        if(princ.flag("activated").getOrElse(false)) Future.successful((true, success(routes.Application.index, "Login erfolgreich.")))
        else Future.successful((false, error(routes.Application.index, "Dein Account ist noch nicht aktiviert.")))
      case None ⇒ Future.successful((false, error(routes.Application.index, "Nutzername / Passwort inkorrekt")))
    }
  }

  def logout = Action { implicit request ⇒
    Redirect(routes.Application.index).withSession(request.session - "authenticatorPrincipal")
  }

  def profile = contextAction1 { implicit requestContext ⇒
    if(!requestContext.principal.isDefined) {
      error(routes.Application.index, "Du musst dich einloggen, bevor du auf dein Profil zugreifen kannst.")
    } else {
      Ok(views.html.Profile())
    }
  }

  def profilePost = asyncContextAction2 { implicit request ⇒ implicit requestContext ⇒
    if(!requestContext.principal.isDefined) {
      Future.successful(
        error(routes.Application.index, "Du musst dich einloggen, bevor du auf dein Profil zugreifen kannst.")
      )
    } else {
      val map = request.body.asFormUrlEncoded.get
      map("section")(0) match {
        case "account" ⇒
          requestContext.principal.get
              .field("firstName", map("firstName")(0))
              .field("lastName", map("lastName")(0))
              .field("university", map("university")(0))
              .save map { _ ⇒
            success(routes.Account.profile, "Accountinformationen aktualisiert.")
          }
        case "cpw" ⇒
          if(map("pw1")(0) == map("pw2")(0)) {
            requestContext.principal.get.cpw(map("pw1")(0)).save map { _ ⇒
              success(routes.Account.profile, "Passwort aktualisiert.")
            }
          } else {
            Future.successful(error(routes.Account.profile, "Passwörter stimmen nicht überein."))
          }
      }
    }
  }

  def register = contextAction1 { implicit requestContext ⇒
    if(!requestContext.principal.isDefined) {
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

      val activationSecret = bytesToHex(Random.nextBytes(16))
      val futurePrinc = auth.principals.create(
        name,
        pw1,
        Map(
          "firstName" -> firstName,
          "lastName" -> lastName,
          "email" -> email,
          "university" -> university,
          "activationSecret" -> activationSecret
        ),
        Map(
          "activated" -> false
        )
      ) map { _.get }

      futurePrinc map { princ ⇒
        val conf = Play.current.configuration
        val baseUri = conf.getString("app.baseUri").get
        val link = baseUri + "/activate/" + princ.name + "/" + activationSecret
        val username = princ.name
        val email = Email(
          "Aktivierung ZaPF-Account",
          conf.getString("app.email").get,
          Seq(princ.field("email").get),
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
      }
    }
  }

  def activate(name: String, key: String) = asyncContextAction2 { implicit request ⇒ implicit requestContext ⇒
    auth.principals.find(name) flatMap {
      case Some(principal) ⇒
        if(principal.field("activationSecret").get == key) {
          principal.flag("activated", true).save map { _ ⇒
            success(routes.Application.index, "Aktivierung erfolgreich.")
          }
        } else {
          Future.successful(error(routes.Application.index, "Ungültige Aktivierungsparameter"))
        }
      case None ⇒
        Future.successful(error(routes.Application.index, "Ungültige Aktivierungsparameter"))
    }
  }
}
