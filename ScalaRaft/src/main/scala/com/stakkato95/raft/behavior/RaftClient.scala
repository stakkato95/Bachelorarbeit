package com.stakkato95.raft.behavior

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.stakkato95.raft.behavior.Leader.Command
import com.stakkato95.raft.behavior.RaftClient.{ClientRequest, ClientResponse, ClientStart, ClientStop}
import com.stakkato95.raft.behavior.base.{BaseCommand, NodesDiscovered}
import com.stakkato95.raft.concurrent.ReentrantPromise

object RaftClient {
  def apply(reentrantPromise: ReentrantPromise[String]): Behavior[BaseCommand] =
    Behaviors.setup(new RaftClient(_, reentrantPromise))

  final case class ClientRequest(value: String, replyTo: ActorRef[ClientResponse]) extends Command

  final case class ClientResponse(currentState: String) extends Command

  final object ClientStart extends Command

  final object ClientStop extends Command

}

class RaftClient(context: ActorContext[BaseCommand],
                 reentrantPromise: ReentrantPromise[String]) extends AbstractBehavior[BaseCommand](context) {

  private var order = 0

  private val cluster = List(
    context.spawnAnonymous(Follower("node-1")),
    context.spawnAnonymous(Follower("node-2")),
    context.spawnAnonymous(Follower("node-3"))
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
    }
  }

  private def onClientStart(): Unit = {
    cluster.foreach(_ ! NodesDiscovered(cluster))
  }

  private def onClientRequest(request: ClientRequest): Unit = {
    // round robin principle
    cluster(order % cluster.size) ! request
    order += 1
  }

  private def onClientResponse(currentState: String): Unit = {
    reentrantPromise.success(currentState)
  }
}
