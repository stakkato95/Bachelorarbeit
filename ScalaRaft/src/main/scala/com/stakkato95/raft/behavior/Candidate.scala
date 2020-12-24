package com.stakkato95.raft.behavior

import akka.actor.typed.{Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

object Candidate {

  def apply(id: String): Behavior[BaseCommand] = Behaviors.setup(new Candidate(_, id))
}

class Candidate(context: ActorContext[BaseCommand], id: String) extends BaseRaftBehavior[BaseCommand](context) {

  context.log.info("{} is candidate", id)
  startElection()

  override def onMessage(msg: BaseCommand): Behavior[BaseCommand] = {
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[BaseCommand]] = {
    case PostStop =>
      this
  }

  private def startElection() = {

  }
}