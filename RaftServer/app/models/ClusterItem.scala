package models

import play.api.libs.json.{Json, OWrites, Reads}

case class ClusterItem(value: String)

object ClusterItem {

  implicit val writes: OWrites[ClusterItem] = Json.writes[ClusterItem]
  implicit val reads: Reads[ClusterItem] = Json.reads[ClusterItem]
}