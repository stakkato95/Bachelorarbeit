package com.stakkato95.raft.behavior.base

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext}
import akka.actor.typed.{ActorRef, Behavior}
import com.stakkato95.raft.{LeaderInfo, LogItem}

import scala.collection.mutable.ArrayBuffer

abstract class BaseRaftBehavior[T](context: ActorContext[T],
                                   protected val nodeId: String,
                                   protected var log: ArrayBuffer[LogItem],
                                   protected var cluster: ArrayBuffer[ActorRef[BaseCommand]]) extends AbstractBehavior[T](context) {

  protected var value: String = _

  protected var lastLeader: Option[LeaderInfo] = None

  override def onMessage(msg: T): Behavior[T] = {
    msg match {
      case NodesDiscovered(nodes) =>
        cluster ++= nodes
        this
    }
  }

  final protected def applyToSimpleStateMachine(item: LogItem) = {
    value = item.value
  }

  final protected def getQuorumSize() = {
    cluster.size / 2 + 1
  }

  final protected def getRestOfCluster() = {
    cluster.filter(_ != context.self)
  }
}
