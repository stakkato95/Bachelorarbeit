package com.stakkato95.raft.behavior

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.stakkato95.raft.{ClusterItem, NodeInfo}
import com.stakkato95.raft.behavior.Leader.Command
import com.stakkato95.raft.behavior.RaftClient.{ClientRequest, ClientResponse, ClientStart, ClientStop}
import com.stakkato95.raft.behavior.base.BaseRaftBehavior.Debug
import com.stakkato95.raft.behavior.base.BaseRaftBehavior.Debug.{LogRequest, LogResponse}
import com.stakkato95.raft.behavior.base.{BaseCommand, NodesDiscovered}
import com.stakkato95.raft.concurrent.ReentrantPromise
import com.stakkato95.raft.log.LogItem

object RaftClient {
  def apply(reentrantPromise: ReentrantPromise[AnyRef]): Behavior[BaseCommand] =
    Behaviors.setup(new RaftClient(_, reentrantPromise))

  final case class ClientRequest(value: String, replyTo: ActorRef[ClientResponse]) extends Command

  final case class ClientResponse(currentState: String) extends Command

  final object ClientStart extends Command

  final object ClientStop extends Command

}

class RaftClient(context: ActorContext[BaseCommand],
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
      case request@Debug.LogRequest(_, _) =>
        onLogRequest(request)
        this
      case Debug.LogResponse(nodeInfo) =>
        onLogResponse(nodeInfo)
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

  private def onLogRequest(logRequest: LogRequest): Unit = {
    val node = cluster.filter(_.nodeId == logRequest.nodeId).head
    node.ref ! logRequest
  }

  private def onLogResponse(nodeInfo: NodeInfo): Unit = {
    reentrantPromise.success(nodeInfo)
  }
}
