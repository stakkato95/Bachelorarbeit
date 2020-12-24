package com.stakkato95.raft.behavior

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext}
import com.stakkato95.raft.{LeaderInfo, LogItem}

import scala.collection.mutable.ArrayBuffer

abstract class BaseRaftBehavior[T](context: ActorContext[T],
                                   nodeId: String,
                                   log: ArrayBuffer[LogItem],
                                   cluster: ArrayBuffer[ActorRef[BaseCommand]]) extends AbstractBehavior[T](context) {

  protected var value: String = _

  protected var lastLeader: Option[LeaderInfo] = None

  override def onMessage(msg: T): Behavior[T] = {
    msg match {
      case NodesDiscovered(nodes) =>
        cluster ++= nodes
        this
    }
  }

  protected def applyToSimpleStateMachine(item: LogItem) = {
    value = item.value
  }
}
