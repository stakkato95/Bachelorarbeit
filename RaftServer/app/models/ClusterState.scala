package models

import play.api.libs.json.{Json, OWrites, Reads}

case class ClusterState(state: String)

object ClusterState {

  implicit val writes: OWrites[ClusterState] = Json.writes[ClusterState]
  implicit val reads: Reads[ClusterState] = Json.reads[ClusterState]
}
