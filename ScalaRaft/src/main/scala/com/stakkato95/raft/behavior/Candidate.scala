package com.stakkato95.raft.behavior

import java.util.concurrent.TimeUnit

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import com.stakkato95.raft.{LastLogItem, LeaderInfo, LogItem}
import com.stakkato95.raft.behavior.Candidate.{Command, ElectionTimerElapsed, RequestVote, VoteGranted}

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
    apply(nodeId, ELECTION_TIMEOUT, log, cluster)
  }

  trait Command extends BaseCommand

  final case class RequestVote(candidateTerm: Int,
                               candidate: ActorRef[Command],
                               lastLogItem: Option[LastLogItem]) extends Command

  final object ElectionTimerElapsed extends Command

  final object VoteGranted extends Command

  private val ELECTION_TIMEOUT = FiniteDuration(1, TimeUnit.SECONDS)
}

class Candidate(context: ActorContext[BaseCommand],
                nodeId: String,
                timer: TimerScheduler[BaseCommand],
                electionTimeout: FiniteDuration,
                log: ArrayBuffer[LogItem],
                cluster: ArrayBuffer[ActorRef[BaseCommand]])
  extends BaseRaftBehavior[BaseCommand](context, nodeId, log, cluster) {

  context.log.info("{} is candidate", nodeId)
  startElection()
  restartElectionTimer()

  private var term: Int = _
  private var votes = 0

  override def onMessage(msg: BaseCommand): Behavior[BaseCommand] = {
    msg match {
      case VoteGranted =>
        onVoteGranted()
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[BaseCommand]] = {
    case PostStop =>
      this
  }

  private def startElection() = {
    term = lastLeader match {
      case None => Leader.INITIAL_TERM + 1
      case Some(LeaderInfo(t, _)) => t + 1
    }
    votes += 1

    val lastItem = log match {
      case ArrayBuffer(_, _*) => Some(LastLogItem(log.size - 1, log.last.leaderTerm))
      case _ => None
    }

    getRestOfCluster().foreach(_ ! RequestVote(term, context.self, lastItem))
  }

  private def restartElectionTimer() = {
    timer.cancelAll()
    timer.startSingleTimer(ElectionTimerElapsed, ElectionTimerElapsed, electionTimeout)
  }

  private def onVoteGranted(): Behavior[BaseCommand] = {
    votes += 1

    if (votes == getQuorumSize()) {
      Leader(nodeId, log, cluster, term)
    } else {
      this
    }
  }
}