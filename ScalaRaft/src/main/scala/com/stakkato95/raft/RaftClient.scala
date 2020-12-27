package com.stakkato95.raft

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.stakkato95.raft.RaftClient.{ClientRequest, ClientStart, ClientStop}
import com.stakkato95.raft.behavior.Follower
import com.stakkato95.raft.behavior.Leader.Command
import com.stakkato95.raft.behavior.base.{BaseCommand, NodesDiscovered}

object RaftClient {
  def apply(resultCallback: String => Unit): Behavior[BaseCommand] = Behaviors.setup(new RaftClient(_, resultCallback))

  final case class ClientRequest(value: String, replyTo: ActorRef[ClientResponse]) extends Command

  final case class ClientResponse(currentState: String) extends Command

  final object ClientStart extends Command

  final object ClientStop extends Command

}

class RaftClient(context: ActorContext[BaseCommand],
                 resultCallback: String => Unit) extends AbstractBehavior[BaseCommand](context) {

  private val node1 = context.spawnAnonymous(Follower("node-1"))
  private val node2 = context.spawnAnonymous(Follower("node-2"))
  private val node3 = context.spawnAnonymous(Follower("node-3"))

  override def onMessage(msg: BaseCommand): Behavior[BaseCommand] = {
    msg match {
      case ClientStart =>
        onClientStart()
        this
      case request@ClientRequest(_, _) =>
        //TODO round robin
        this
      case ClientStop =>
        Behaviors.stopped
    }
  }

  private def onClientStart(): Unit = {
    val cluster = List(node1.ref, node2.ref, node3.ref)
    node1 ! NodesDiscovered(cluster)
    node2 ! NodesDiscovered(cluster)
    node3 ! NodesDiscovered(cluster)
  }
}
