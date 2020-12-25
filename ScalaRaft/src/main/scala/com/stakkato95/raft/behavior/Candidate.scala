package com.stakkato95.raft.behavior

import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import com.stakkato95.raft.behavior.Candidate.{ElectionTimerElapsed, RequestVote, VoteGranted}
import com.stakkato95.raft.behavior.Follower.{AppendEntriesHeartbeat, AppendEntriesNewLog}
import com.stakkato95.raft.behavior.base.{BaseCommand, BaseRaftBehavior}
import com.stakkato95.raft.{LastLogItem, LeaderInfo, LogItem}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration
import scala.util.Random

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

  private val rnd = new Random()

  private val MAX_ELECTION_TIMEOUT = 3
  private val MIN_ELECTION_TIMEOUT = 1

  def ELECTION_TIMEOUT: FiniteDuration = {
    val length = MIN_ELECTION_TIMEOUT + (rnd.nextFloat() * (MAX_ELECTION_TIMEOUT - MIN_ELECTION_TIMEOUT)).toLong
    FiniteDuration(length, TimeUnit.SECONDS)
  }
}

class Candidate(context: ActorContext[BaseCommand],
                nodeId: String,
                timer: TimerScheduler[BaseCommand],
                electionTimeout: FiniteDuration,
                candidateLog: ArrayBuffer[LogItem],
                candidateCluster: ArrayBuffer[ActorRef[BaseCommand]])
  extends BaseRaftBehavior[BaseCommand](context, nodeId, candidateLog, candidateCluster) {

  private var term: Option[Int] = None
  private var votes = 0

  context.log.info("{} is candidate with election timeout {}", nodeId, electionTimeout)
  restartElectionProcess()

  override def onMessage(msg: BaseCommand): Behavior[BaseCommand] = {
    msg match {
      case VoteGranted =>
        onVoteGranted()
      case AppendEntriesHeartbeat(leaderInfo) =>
        onAppendEntries(leaderInfo)
      case AppendEntriesNewLog(leaderInfo, _, _, _, _) =>
        onAppendEntries(leaderInfo)
      case ElectionTimerElapsed =>
        restartElectionProcess()
        this
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[BaseCommand]] = {
    case PostStop =>
      this
  }

  private def restartElectionProcess() = {
    startElection()
    startElectionTimer()
  }

  private def startElection() = {
    if (term.isDefined) {
      term = term.map(_ + 1)
    } else {
      term = lastLeader match {
        case None => Some(Leader.INITIAL_TERM + 1) //there was no Leader before
        case Some(LeaderInfo(t, _)) if term.isEmpty => Some(t + 1) //there was Leader before
      }
    }

    votes = 1

    val lastItem = candidateLog match {
      case ArrayBuffer(_, _*) => Some(LastLogItem(candidateLog.size - 1, candidateLog.last.leaderTerm))
      case _ => None
    }

    getRestOfCluster().foreach(_ ! RequestVote(term.get, context.self, lastItem))
  }

  private def startElectionTimer() = {
    timer.startSingleTimer(ElectionTimerElapsed, ElectionTimerElapsed, electionTimeout)
  }

  private def onVoteGranted(): Behavior[BaseCommand] = {
    votes += 1

    if (votes == getQuorumSize()) {
      timer.cancel(ElectionTimerElapsed)
      Leader(nodeId, candidateLog, candidateCluster, term.get)
    } else {
      this
    }
  }

  def onAppendEntries(leaderInfo: LeaderInfo): Behavior[BaseCommand] = {
    val t = term match {
      case None => Leader.INITIAL_TERM
      case Some(value) => value
    }

    if (leaderInfo.term >= t) {
      Follower(nodeId, log, cluster)
    } else {
      this
    }
  }
}