package services

import akka.actor.typed.ActorSystem
import com.stakkato95.raft.behavior.Client.{ClientRequest, ClientStart}
import com.stakkato95.raft.behavior.base.BaseCommand
import com.stakkato95.raft.behavior.{Candidate, Client, Follower, Leader}
import com.stakkato95.raft.concurrent.{ReentrantFuture, ReentrantPromise}
import com.stakkato95.raft.debug.transport.{CandidateDebugInfo, FollowerDebugInfo, LeaderDebugInfo}
import javax.inject.Inject
import models.{ClusterItem, ClusterState}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.collection.mutable.ListBuffer

class ClusterService @Inject()(actorSystem: akka.actor.ActorSystem)(implicit executionContext: ExecutionContext) {

  private var replicationPromise: Option[ReentrantPromise[String]] = None
  private var leaderPromise: Option[ReentrantPromise[LeaderDebugInfo]] = None
  private var followersPromise: Option[ReentrantPromise[FollowerDebugInfo]] = None
  private var candidatesPromise: Option[ReentrantPromise[CandidateDebugInfo]] = None

  private var replicationFuture: Option[ReentrantFuture[String]] = None
  private var leaderFuture: Option[ReentrantFuture[LeaderDebugInfo]] = None
  private var followersFuture: Option[ReentrantFuture[FollowerDebugInfo]] = None
  private var candidatesFuture: Option[ReentrantFuture[CandidateDebugInfo]] = None

  private var raftActorSystem: Option[ActorSystem[BaseCommand]] = None

  actorSystem.scheduler.scheduleOnce(5 seconds) {
    replicationPromise = Some(new ReentrantPromise[String]())
    leaderPromise = Some(new ReentrantPromise[LeaderDebugInfo]())
    followersPromise = Some(new ReentrantPromise[FollowerDebugInfo]())
    candidatesPromise = Some(new ReentrantPromise[CandidateDebugInfo]())
    replicationFuture = Some(replicationPromise.get.future)
    leaderFuture = Some(leaderPromise.get.future)
    followersFuture = Some(followersPromise.get.future)
    candidatesFuture = Some(candidatesPromise.get.future)

    raftActorSystem = Some(ActorSystem(
      Client(replicationPromise.get, leaderPromise.get, followersPromise.get, candidatesPromise.get),
      "client"
    ))
    raftActorSystem.get ! ClientStart
  }

  def addItemToCluster(item: ClusterItem): Unit = {
    if (raftActorSystem.isEmpty) {
      return
    }

    raftActorSystem.get ! ClientRequest(item.value, raftActorSystem.get.ref)
  }

  def getClusterState(): ClusterState = {
    if (raftActorSystem.isEmpty) {
      return ClusterState(
        leader = None,
        candidates = ListBuffer(),
        followers = ListBuffer()
      )
    }

    raftActorSystem.get ! Leader.Debug.InfoRequest(raftActorSystem.get.ref)
    val leaderInfo = leaderFuture.get.getWithTimeout(ClusterService.FUTURE_TIMEOUT_MILLISEC)

    val candidatesInfo = ListBuffer[Option[CandidateDebugInfo]]()
    raftActorSystem.get ! Candidate.Debug.InfoRequest(raftActorSystem.get.ref)
    candidatesInfo += candidatesFuture.get.getWithTimeout(ClusterService.FUTURE_TIMEOUT_MILLISEC)
    candidatesInfo += candidatesFuture.get.getWithTimeout(ClusterService.FUTURE_TIMEOUT_MILLISEC)
    candidatesInfo += candidatesFuture.get.getWithTimeout(ClusterService.FUTURE_TIMEOUT_MILLISEC)

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
    raftActorSystem.get ! Follower.Debug.InfoRequest(nodeId, raftActorSystem.get.ref)
    followersInfo += followersFuture.get.getWithTimeout(ClusterService.FUTURE_TIMEOUT_MILLISEC)
  }
}

object ClusterService {

  private val FUTURE_TIMEOUT_MILLISEC = 10

}