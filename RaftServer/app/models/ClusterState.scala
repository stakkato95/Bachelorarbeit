package models

import com.stakkato95.raft.debug.{DebugFiniteDuration, DebugPendingItem, LogDebugInfo}
import com.stakkato95.raft.debug.transport.{CandidateDebugInfo, FollowerDebugInfo, LeaderDebugInfo}
import com.stakkato95.raft.log.LogItem
import play.api.libs.json.{Format, Json, OWrites, Reads}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.{FiniteDuration, TimeUnit}

case class ClusterState(leader: Option[LeaderDebugInfo],
                        candidates: ListBuffer[Option[CandidateDebugInfo]],
                        followers: ListBuffer[Option[FollowerDebugInfo]])

object ClusterState {

  //LeaderDebugInfo
  implicit val writesLogItem: OWrites[LogItem] = Json.writes[LogItem]
  implicit val writesLogDebugInfo: OWrites[LogDebugInfo] = Json.writes[LogDebugInfo]
  implicit val writesDebugPendingItem: OWrites[DebugPendingItem] = Json.writes[DebugPendingItem]
  implicit val writesLeaderDebugInfo: OWrites[LeaderDebugInfo] = Json.writes[LeaderDebugInfo]
  implicit val readsLogItem: Reads[LogItem] = Json.reads[LogItem]
  implicit val readsLogDebugInfo: Reads[LogDebugInfo] = Json.reads[LogDebugInfo]
  implicit val readsDebugPendingItem: Reads[DebugPendingItem] = Json.reads[DebugPendingItem]
  implicit val readsLeaderDebugInfo: Reads[LeaderDebugInfo] = Json.reads[LeaderDebugInfo]

  //CandidateDebugInfo
  implicit val writesCandidateDebugInfo: OWrites[CandidateDebugInfo] = Json.writes[CandidateDebugInfo]
  implicit val writesDebugFiniteDuration: OWrites[DebugFiniteDuration] = Json.writes[DebugFiniteDuration]
  implicit val readsCandidateDebugInfo: Reads[CandidateDebugInfo] = Json.reads[CandidateDebugInfo]
  implicit val readsDebugFiniteDuration: Reads[DebugFiniteDuration] = Json.reads[DebugFiniteDuration]
  implicit val formatCandidateDebugInfo: Format[Option[CandidateDebugInfo]] = Format.optionWithNull(Json.format[CandidateDebugInfo])

  //FollowerDebugInfo
  implicit val writesFollowerDebugInfo: OWrites[FollowerDebugInfo] = Json.writes[FollowerDebugInfo]
  implicit val readsFollowerDebugInfo: Reads[FollowerDebugInfo] = Json.reads[FollowerDebugInfo]
  implicit val formatFollowerDebugInfo: Format[Option[FollowerDebugInfo]] = Format.optionWithNull(Json.format[FollowerDebugInfo])

  implicit val writesClusterState: OWrites[ClusterState] = Json.writes[ClusterState]
  implicit val readsClusterState: Reads[ClusterState] = Json.reads[ClusterState]
}

object FiniteDuration {

  def unapply(duration: FiniteDuration): Some[(Long, TimeUnit)] = {
    Some((duration._1, duration._2))
  }

}