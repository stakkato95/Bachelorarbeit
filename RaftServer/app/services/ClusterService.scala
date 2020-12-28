package services

import akka.actor.typed.ActorSystem
import com.stakkato95.raft.behavior.Client.{ClientRequest, ClientStart}
import com.stakkato95.raft.behavior.{Client, Leader}
import com.stakkato95.raft.concurrent.ReentrantPromise
import com.stakkato95.raft.debug.{CandidateDebugInfo, FollowerDebugInfo, LeaderDebugInfo}
import javax.inject.Inject
import models.{ClusterItem, ClusterState, StateMachineState}

class ClusterService @Inject()() {

  private val promise = new ReentrantPromise[AnyRef]()
  private val future = promise.future
  private val actorSystem = ActorSystem(Client(promise), "client")
  actorSystem ! ClientStart

  def addItemToCluster(item: ClusterItem): StateMachineState = {
    actorSystem ! ClientRequest(item.value, actorSystem.ref)

    future.get[String]() match {
      case Some(clusterState) =>
        StateMachineState(state = clusterState)
      case None =>
        StateMachineState(state = "no state")
    }
  }

  def getClusterState(): ClusterState = {
    actorSystem ! Leader.Debug.InfoRequest(actorSystem.ref)
    val leaderInfo = future.getWithTimeout[LeaderDebugInfo](ClusterService.FUTURE_TIMEOUT_MILLISEC)
    val candidatesInfo = List(
      future.getWithTimeout[CandidateDebugInfo](ClusterService.FUTURE_TIMEOUT_MILLISEC),
      future.getWithTimeout[CandidateDebugInfo](ClusterService.FUTURE_TIMEOUT_MILLISEC),
      future.getWithTimeout[CandidateDebugInfo](ClusterService.FUTURE_TIMEOUT_MILLISEC)
    )
    val followersInfo = List(
      future.getWithTimeout[FollowerDebugInfo](ClusterService.FUTURE_TIMEOUT_MILLISEC),
      future.getWithTimeout[FollowerDebugInfo](ClusterService.FUTURE_TIMEOUT_MILLISEC),
      future.getWithTimeout[FollowerDebugInfo](ClusterService.FUTURE_TIMEOUT_MILLISEC)
    )

    ClusterState(
      leader = leaderInfo,
      candidates = candidatesInfo,
      followers = followersInfo
    )
  }
}

object ClusterService {

  private val FUTURE_TIMEOUT_MILLISEC = 500

}