package models

import play.api.libs.json.{Json, OWrites, Reads}

case class StateMachineState(state: String)

object StateMachineState {

  implicit val writes: OWrites[StateMachineState] = Json.writes[StateMachineState]
  implicit val reads: Reads[StateMachineState] = Json.reads[StateMachineState]
}
