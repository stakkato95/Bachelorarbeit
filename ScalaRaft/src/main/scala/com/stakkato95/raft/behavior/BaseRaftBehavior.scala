package com.stakkato95.raft.behavior

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext}
import com.stakkato95.raft.LogItem

abstract class BaseRaftBehavior[T](context: ActorContext[T]) extends AbstractBehavior[T](context) {

  protected var value: String = _

  protected def applyToSimpleStateMachine(item: LogItem) = {
    value = item.value
  }
}
