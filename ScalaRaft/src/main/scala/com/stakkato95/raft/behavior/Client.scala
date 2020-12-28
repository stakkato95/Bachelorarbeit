package com.stakkato95.raft.behavior

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.stakkato95.raft.behavior.Client.{ClientRequest, ClientResponse, ClientStart, ClientStop}
import com.stakkato95.raft.behavior.Follower.Debug.InfoRequest
import com.stakkato95.raft.behavior.Leader.Command
import com.stakkato95.raft.behavior.Leader.Debug.LeaderInfoRequest
import com.stakkato95.raft.behavior.base.BaseRaftBehavior.Debug.NodeInfoRequest
import com.stakkato95.raft.behavior.base.{BaseCommand, BaseRaftBehavior, NodesDiscovered}
import com.stakkato95.raft.concurrent.ReentrantPromise
import com.stakkato95.raft.{ClusterItem, FollowerInfo, LeaderDebugInfo, NodeInfo}

object Client {
  def apply(reentrantPromise: ReentrantPromise[AnyRef]): Behavior[BaseCommand] =
    Behaviors.setup(new Client(_, reentrantPromise))

  final case class ClientRequest(value: String, replyTo: ActorRef[ClientResponse]) extends Command

  final case class ClientResponse(currentState: String) extends Command

  final object ClientStart extends Command

  final object ClientStop extends Command

}

class Client(context: ActorContext[BaseCommand],
             reentrantPromise: ReentrantPromise[AnyRef]) extends AbstractBehavior[BaseCommand](context) {

  private var order = 0

  private val cluster = List(
    ClusterItem("node-1", context.spawnAnonymous(Follower("node-1"))),
    ClusterItem("node-2", context.spawnAnonymous(Follower("node-2"))),
    ClusterItem("node-3", context.spawnAnonymous(Follower("node-3")))
  )

  override def onMessage(msg: BaseCommand): Behavior[BaseCommand] = {
    msg match {
      case ClientStart =>
        onClientStart()
        this
      case request@ClientRequest(_, _) =>
        onClientRequest(request)
        this
      case ClientResponse(currentState) =>
        onClientResponse(currentState)
        this
      case ClientStop =>
        Behaviors.stopped
      //
      case request@BaseRaftBehavior.Debug.NodeInfoRequest(_, _) =>
        onLogRequest(request)
        this
      case BaseRaftBehavior.Debug.NodeInfoResponse(nodeInfo) =>
        onLogResponse(nodeInfo)
        this
      //
      case request@Leader.Debug.LeaderInfoRequest(_) =>
        onLeaderInfoRequest(request)
        this
      case Leader.Debug.LeaderInfoResponse(leaderDebugInfo) =>
        onLeaderInfoResponse(leaderDebugInfo)
        this
      //
      case request@Follower.Debug.InfoRequest(_, _) =>
        onFollowerInfoRequest(request)
        this
      case Follower.Debug.InfoReply(followerInfo) =>
        onFollowerInfoResponse(followerInfo)
        this
    }
  }

  private def onClientStart(): Unit = {
    val refs = cluster.map(_.ref)
    cluster.foreach(_.ref ! NodesDiscovered(refs))
  }

  private def onClientRequest(request: ClientRequest): Unit = {
    // round robin principle
    cluster(order % cluster.size).ref ! request
    order += 1
  }

  private def onClientResponse(currentState: String): Unit = {
    reentrantPromise.success(currentState)
  }

  //
  private def onLogRequest(logRequest: NodeInfoRequest): Unit = {
    getActorWithId(logRequest.nodeId) ! logRequest
  }

  private def onLogResponse(nodeInfo: NodeInfo): Unit = {
    reentrantPromise.success(nodeInfo)
  }

  //
  private def onLeaderInfoRequest(request: LeaderInfoRequest): Unit = {
    cluster.foreach(_.ref ! request)
  }

  private def onLeaderInfoResponse(leaderDebugInfo: LeaderDebugInfo): Unit = {
    reentrantPromise.success(leaderDebugInfo)
  }

  //
  private def onFollowerInfoRequest(request: Follower.Debug.InfoRequest): Unit = {
    getActorWithId(request.nodeId) ! request
  }

  private def onFollowerInfoResponse(followerInfo: FollowerInfo): Unit = {
    reentrantPromise.success(followerInfo)
  }

  private def getActorWithId(nodeId: String): ActorRef[BaseCommand] = {
    cluster.filter(_.nodeId == nodeId).head.ref
  }
}
