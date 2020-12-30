package models

import play.api.libs.json.{Json, OWrites, Reads}

case class StateMachineValue(value: String)

object StateMachineValue {

  implicit val writes: OWrites[StateMachineValue] = Json.writes[StateMachineValue]
  implicit val reads: Reads[StateMachineValue] = Json.reads[StateMachineValue]
}
