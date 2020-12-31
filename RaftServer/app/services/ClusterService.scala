package services

import akka.actor.typed.ActorSystem
import com.stakkato95.raft.behavior.Client.{ClientRequest, ClientStart}
import com.stakkato95.raft.behavior.{Candidate, Client, Follower, Leader}
import com.stakkato95.raft.concurrent.ReentrantPromise
import com.stakkato95.raft.debug.transport.{CandidateDebugInfo, FollowerDebugInfo, LeaderDebugInfo}
import javax.inject.Inject
import models.{ClusterItem, ClusterState, StateMachineValue}

import scala.collection.mutable.ListBuffer

class ClusterService @Inject()() {

  private val promise = new ReentrantPromise[AnyRef]()
  private val future = promise.future
  private val actorSystem = ActorSystem(Client(promise), "client")
  actorSystem ! ClientStart

  def addItemToCluster(item: ClusterItem): StateMachineValue = {
    actorSystem ! ClientRequest(item.value, actorSystem.ref)

    future.get[String]() match {
      case Some(clusterState) =>
        StateMachineValue(value = clusterState)
      case None =>
        StateMachineValue(value = "no state")
    }
  }

  def getClusterState(): ClusterState = {
    actorSystem ! Leader.Debug.InfoRequest(actorSystem.ref)
    val leaderInfo = future.getWithTimeout[LeaderDebugInfo](ClusterService.FUTURE_TIMEOUT_MILLISEC)

    val candidatesInfo = ListBuffer[Option[CandidateDebugInfo]]()
    actorSystem ! Candidate.Debug.InfoRequest(actorSystem.ref)
    candidatesInfo += future.getWithTimeout[CandidateDebugInfo](ClusterService.FUTURE_TIMEOUT_MILLISEC)
    candidatesInfo += future.getWithTimeout[CandidateDebugInfo](ClusterService.FUTURE_TIMEOUT_MILLISEC)
    candidatesInfo += future.getWithTimeout[CandidateDebugInfo](ClusterService.FUTURE_TIMEOUT_MILLISEC)

    val followersInfo = ListBuffer[Option[FollowerDebugInfo]]()
    requestFollowerInfo(followersInfo, "node-1")
    requestFollowerInfo(followersInfo, "node-2")
    requestFollowerInfo(followersInfo, "node-3")

    ClusterState(
      leader = leaderInfo,
      candidates = candidatesInfo.filter(_.isDefined),
      followers = followersInfo.filter(_.isDefined)
    )
  }

  private def requestFollowerInfo(followersInfo: ListBuffer[Option[FollowerDebugInfo]], nodeId: String): Unit = {
    actorSystem ! Follower.Debug.InfoRequest(nodeId, actorSystem.ref)
    followersInfo += future.getWithTimeout[FollowerDebugInfo](ClusterService.FUTURE_TIMEOUT_MILLISEC)
  }
}

object ClusterService {

  private val FUTURE_TIMEOUT_MILLISEC = 10

}