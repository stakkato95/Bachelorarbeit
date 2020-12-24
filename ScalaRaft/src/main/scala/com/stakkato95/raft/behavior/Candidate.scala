package com.stakkato95.raft.behavior

import akka.actor.typed.{Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object Candidate {

  def apply(id: String): Behavior[Command] = Behaviors.setup(new Candidate(_, id))
}

class Candidate(context: ActorContext[Command], id: String) extends AbstractBehavior[Command](context) {

  context.log.info("{} is candidate", id)
  startElection()

  override def onMessage(msg: Command): Behavior[Command] = {
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      this
  }

  private def startElection() = {

  }
}
