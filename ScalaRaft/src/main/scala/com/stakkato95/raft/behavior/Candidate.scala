package com.stakkato95.raft.behavior

import java.util.concurrent.TimeUnit

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import com.stakkato95.raft.{LastLogItem, LeaderInfo, LogItem}
import com.stakkato95.raft.behavior.Candidate.{Command, ElectionTimerElapsed, RequestVote}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration

object Candidate {

  def apply(nodeId: String,
            timeout: FiniteDuration,
            log: ArrayBuffer[LogItem],
            cluster: ArrayBuffer[ActorRef[BaseCommand]]): Behavior[BaseCommand] = {
    Behaviors.setup { context =>
      Behaviors.withTimers { timer =>
        new Candidate(context, nodeId, timer, timeout, log, cluster)
      }
    }
  }

  def apply(nodeId: String,
            log: ArrayBuffer[LogItem],
            cluster: ArrayBuffer[ActorRef[BaseCommand]]): Behavior[BaseCommand] = {
    Behaviors.setup { context =>
      Behaviors.withTimers { timer =>
        new Candidate(context, nodeId, timer, TIMEOUT, log, cluster)
      }
    }
  }

  trait Command extends BaseCommand

  case class RequestVote(candidateTerm: Int,
                         candidate: ActorRef[Command],
                         lastLogItem: Option[LastLogItem]) extends Command

  object ElectionTimerElapsed extends Command

  private val TIMEOUT = FiniteDuration(1, TimeUnit.SECONDS)
}

class Candidate(context: ActorContext[BaseCommand],
                nodeId: String,
                timers: TimerScheduler[BaseCommand],
                timeout: FiniteDuration,
                log: ArrayBuffer[LogItem],
                cluster: ArrayBuffer[ActorRef[BaseCommand]])
  extends BaseRaftBehavior[BaseCommand](context, nodeId, log, cluster) {

  context.log.info("{} is candidate", nodeId)
  startElection()
  restartElectionTimer()

  private var term = Leader.INITIAL_TERM
  private var votes = 0

  override def onMessage(msg: BaseCommand): Behavior[BaseCommand] = {
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[BaseCommand]] = {
    case PostStop =>
      this
  }

  private def startElection() = {
    term = lastLeader match {
      case None => Leader.INITIAL_TERM
      case Some(LeaderInfo(term, _)) => term
    }
    votes += 1

    val lastItem = log match {
      case ArrayBuffer(_, _*) => Some(LastLogItem(log.size - 1, log.last.leaderTerm))
      case _ => None
    }

    cluster
      .filter(_ != context.self)
      .foreach(_ ! RequestVote(term, context.self, lastItem))
  }

  private def restartElectionTimer() = {
    timers.cancelAll()
    timers.startSingleTimer(ElectionTimerElapsed, ElectionTimerElapsed, timeout)
  }
}