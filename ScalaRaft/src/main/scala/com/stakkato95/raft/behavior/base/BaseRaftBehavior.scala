package com.stakkato95.raft.behavior.base

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext}
import akka.actor.typed.{ActorRef, Behavior}
import com.stakkato95.raft.LeaderInfo
import com.stakkato95.raft.log.{LogItem, PreviousLogItem}

import scala.collection.mutable.ArrayBuffer

abstract class BaseRaftBehavior[T](context: ActorContext[T],
                                   protected val nodeId: String,
                                   protected var log: ArrayBuffer[LogItem],
                                   protected var cluster: ArrayBuffer[ActorRef[BaseCommand]],
                                   protected var currentStateMachineValue: String) extends AbstractBehavior[T](context) {

  protected var lastLeader: Option[LeaderInfo] = None

  override def onMessage(msg: T): Behavior[T] = {
    msg match {
      case NodesDiscovered(nodes) =>
        cluster ++= nodes
        this
      case _ =>
        //TODO dangerous!!! all non-matched messages are skipped
        this
    }
  }

  final protected def applyToSimpleStateMachine(item: LogItem) = {
    currentStateMachineValue += item.value
  }

  final protected def unapplyFromSimpleStateMachine() = {
    currentStateMachineValue = currentStateMachineValue.substring(0, currentStateMachineValue.size - 1)
  }

  final protected def quorumSize: Int = {
    cluster.size / 2 + 1
  }

  final protected def getRestOfCluster() = {
    cluster.filter(_ != context.self)
  }

  final protected def previousLogItem: Option[PreviousLogItem] = {
    log match {
      case ArrayBuffer(_, _*) => Some(PreviousLogItem(log.size - 1, log.last.leaderTerm))
      case _ => None
    }
  }
}