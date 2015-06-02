package models

import play.libs.Akka
import play.api.Play
import reactivemongo.api._
import reactivemongo.api.collections.default.BSONCollection
import play.api.libs.concurrent.Execution.Implicits._

object Mongo {

  private val conf = Play.current.configuration
  private val host: String = conf.getString("mongo.host").get
  private val db: String = conf.getString("mongo.db").get
  private val user: Option[String] = conf.getString("mongo.user")
  private val pass: Option[String] = conf.getString("mongo.pass")

  private val driver = new MongoDriver(Akka.system)
  private val connection = driver.connection(Seq(host))
  private val database = connection(db)

  def collection(coll: String): BSONCollection = database.collection[BSONCollection](coll)
}
