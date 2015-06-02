package models;

import xyz.wiedenhoeft.scalacrypt._
import khash.{ HmacSHA256, PBKDF2 }
import play.api.libs.json._
import reactivemongo.bson._

case class PasswordHash(
  hash: Seq[Byte],
  salt: Seq[Byte],
  iterations: Int
) {

  def verify(password: String): Boolean = {
    val hashAlgorithm = PBKDF2(HmacSHA256, iterations, hash.length)
    hashAlgorithm(password.getBytes.toSeq.toKey[SymmetricKeyArbitrary].get, salt).get == hash
  }
}

object PasswordHash {

  implicit val bsonReader = new BSONReader[BSONDocument, PasswordHash] {
    def read(bson: BSONDocument): PasswordHash = {
      PasswordHash(
        {
          val buffer = bson.getAs[BSONBinary]("hash").get.value
          buffer.readArray(buffer.size).toSeq
        },
        {
          val buffer = bson.getAs[BSONBinary]("salt").get.value
          buffer.readArray(buffer.size).toSeq
        },
        bson.getAs[Int]("iterations").get
      )
    }
  }

  implicit val bsonWriter = new BSONWriter[PasswordHash, BSONDocument] {
    def write(pw: PasswordHash): BSONDocument = {
      BSONDocument(
        "hash" -> BSONBinary(pw.hash.toArray, Subtype.GenericBinarySubtype),
        "salt" -> BSONBinary(pw.salt.toArray, Subtype.GenericBinarySubtype),
        "iterations" -> pw.iterations
      )
    }
  }

  val defaultIterations = 20000
  val defaultLength = 32
  val hashAlgorithm: KeyedHash[Key] =
    PBKDF2(HmacSHA256, defaultIterations, defaultLength)

  def create(password: String): PasswordHash = {
    val salt = Random.nextBytes(defaultLength)
    val hash = hashAlgorithm(password.getBytes.toSeq.toKey[SymmetricKeyArbitrary].get, salt).get
    PasswordHash(hash, salt, defaultIterations)
  }

  def createRandom: PasswordHash = {
    create(Random.nextBytes(128).toString)
  }
}
