package models

import play.api.libs.concurrent.Execution.Implicits._
import reactivemongo.bson._
import reactivemongo.api._
import scala.concurrent._
import scala.util.{ Try, Success, Failure }
import scala.util.{ Either, Left, Right }
import play.api.Play

case class User(
  name: String,
  pass: Either[PasswordHash, String],
  firstName: String,
  lastName: String,
  email: String,
  university: String
) {

  def update: Future[Try[User]] = {
    Mongo.collection("users").update(BSONDocument("name" -> name), User.bsonWriter.write(this)) map { lastError ⇒
      if(lastError.ok) {
        Success(this)
      } else {
        Failure(lastError)
      }
    }
  }
}

object User {

  implicit val bsonReader = new BSONReader[BSONDocument, User] {
    def read(bson: BSONDocument): User = {
      User(
        bson.getAs[String]("name").get,
        Left(bson.getAs[PasswordHash]("pass").get),
        bson.getAs[String]("firstName").get,
        bson.getAs[String]("lastName").get,
        bson.getAs[String]("email").get,
        bson.getAs[String]("university").get
      )
    }
  }

  implicit val bsonWriter = new BSONWriter[User, BSONDocument] {
    def write(user: User): BSONDocument = {
      BSONDocument(
        "name" -> user.name,
        "pass" -> user.pass.left.get,
        "firstName" -> user.firstName,
        "lastName" -> user.lastName,
        "email" -> user.email,
        "university" -> user.university
      )
    }
  }

  def ensureSetup: Unit = {
    // Ensure that “name” is unique
    Mongo.collection("users").indexesManager.ensure(indexes.Index(Seq(("name", indexes.IndexType.Text)), unique = true)) map {
      case false ⇒ throw new Exception("Can not set index for users")
      case true ⇒
    }

    // Insert admin if it does not exist
    Play.current.configuration.getString("admin.initpass") match {
      case Some(pass) ⇒ register(User("admin", Right(pass), "Admin", "Istrator", "root@localhost", "Administrations Uni")) map {
        case Success(_) ⇒
        case Failure(f) ⇒ throw f
      }
      case None ⇒
    }
  }

  
  def findByName(name: String): Future[Option[User]] = {
    val coll = Mongo.collection("users")
    coll.find(BSONDocument("name" -> name)).cursor[BSONDocument].collect[Seq]() map { bsonList ⇒
      val userList = bsonList map { _.as[User] }
      if(userList.length > 1) {
        throw new Exception("Database inconsistent. Multiple users of the same name encountered.")
      } else if(userList.length == 1) {
        Some(userList(0))
      } else {
        None
      }
    }
  }

  def register(register: User): Future[Try[User]] = {
    val coll = Mongo.collection("users")
    val user = register.copy(pass = register.pass match {
      case Left(pass) ⇒ Left(pass)
      case Right(pass) ⇒ Left(PasswordHash.create(pass))
    })
    findByName(user.name) flatMap {
      case Some(_) ⇒
        Future.successful(Failure[User](new Exception("User already exists")))
      case None ⇒
        coll.insert(User.bsonWriter.write(user)) map { lastError ⇒
          if(lastError.ok) Success(user)
          else Failure(lastError)
        }
    }
  }
}
