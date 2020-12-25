package com.stakkato95.raft.behavior.base

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext}
import akka.actor.typed.{ActorRef, Behavior}
import com.stakkato95.raft.{LastLogItem, LeaderInfo, LogItem}

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

  final protected def quorumSize: Int = {
    cluster.size / 2 + 1
  }

  final protected def getRestOfCluster() = {
    cluster.filter(_ != context.self)
  }

  final protected def previousLogItem: Option[LastLogItem] = {
    log match {
      case ArrayBuffer(_, _*) => Some(LastLogItem(log.size - 1, log.last.leaderTerm))
      case _ => None
    }
  }
}
