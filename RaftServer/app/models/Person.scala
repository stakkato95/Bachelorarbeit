package models

import play.api.libs.json._

case class Person(name: String, age: Int)

object Person {

  implicit val writes: OWrites[Person] = Json.writes[Person]
  implicit val reads: Reads[Person] = Json.reads[Person]
}
