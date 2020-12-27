package com.stakkato95.raft

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.stakkato95.raft.behavior.Follower
import com.stakkato95.raft.behavior.base.NodesDiscovered

object ActorSystemGuardian {
  def apply(): Behavior[String] = Behaviors.setup(new ActorSystemGuardian(_))
}

class ActorSystemGuardian(context: ActorContext[String]) extends AbstractBehavior[String](context) {

  private val node1 = context.spawnAnonymous(Follower("node-1"))
  private val node2 = context.spawnAnonymous(Follower("node-2"))
  private val node3 = context.spawnAnonymous(Follower("node-3"))

  override def onMessage(msg: String): Behavior[String] = {
    msg match {
      case "start" =>
        val cluster = List(node1.ref, node2.ref, node3.ref)
        node1 ! NodesDiscovered(cluster)
        node2 ! NodesDiscovered(cluster)
        node3 ! NodesDiscovered(cluster)
        this
      case "stop" =>
        Behaviors.stopped
    }
  }
}
