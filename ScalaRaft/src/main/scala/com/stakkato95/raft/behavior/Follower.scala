package com.stakkato95.raft.behavior

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import com.stakkato95.raft.behavior.Candidate.{Command, RequestVote, VoteGranted}
import com.stakkato95.raft.behavior.Follower._
import com.stakkato95.raft.behavior.Leader.AppendEntriesResponse
import com.stakkato95.raft.behavior.base.{BaseCommand, BaseRaftBehavior}
import com.stakkato95.raft.{LastLogItem, LeaderInfo, LogItem}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration

object Follower {

  def apply(nodeId: String,
            timeout: FiniteDuration,
            log: ArrayBuffer[LogItem],
            cluster: ArrayBuffer[ActorRef[BaseCommand]]): Behavior[BaseCommand] = {
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        new Follower(context, nodeId, timers, timeout, log, cluster)
      }
    }
  }

  def apply(nodeId: String,
            log: ArrayBuffer[LogItem],
            cluster: ArrayBuffer[ActorRef[BaseCommand]]): Behavior[BaseCommand] = {
    apply(nodeId, Candidate.ELECTION_TIMEOUT, log, cluster)
  }

  trait Command extends BaseCommand

  //TODO heartbeat also includes "leaderCommit: Int" !!!
  final case class AppendEntriesHeartbeat(leaderInfo: LeaderInfo) extends Command

  final case class AppendEntriesNewLog(leaderInfo: LeaderInfo,
                                       previousLogItem: Option[LastLogItem],
                                       newLogItem: String,
                                       leaderCommit: Int,
                                       logItemUuid: String) extends Command

  private final object HeartbeatTimerElapsed extends Command

}

class Follower(context: ActorContext[BaseCommand],
               followerNodeId: String,
               timer: TimerScheduler[BaseCommand],
               heartBeatTimeout: FiniteDuration,
               followerLog: ArrayBuffer[LogItem],
               followerCluster: ArrayBuffer[ActorRef[BaseCommand]])
  extends BaseRaftBehavior[BaseCommand](context, followerNodeId, followerLog, followerCluster) {

  context.log.info("{} is follower", followerNodeId)
  restartHeartbeatTimer()

  //TODO reset to "None" when new leader established with AppendEntriesHeartbeat
  private var grantedVote: Option[Int] = None

  override def onMessage(msg: BaseCommand): Behavior[BaseCommand] = {
    msg match {
      case AppendEntriesHeartbeat(leaderInfo) =>
        onHeartbeat(leaderInfo)
        this
      case AppendEntriesNewLog(leaderInfo, previousLogItem, newLogItem, leaderCommit, logItemUuid) =>
        onAppendNewLogItem(leaderInfo, previousLogItem, newLogItem, leaderCommit, logItemUuid)
        this
      case RequestVote(candidateTerm, candidate, lastLogItem) =>
        onRequestVote(candidateTerm, candidate, lastLogItem)
        this
      case HeartbeatTimerElapsed =>
        onHeartbeatTimerElapsed()
      case _ =>
        super.onMessage(msg)
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[BaseCommand]] = {
    case PostStop =>
      context.log.info("{} stop", followerNodeId)
      this
  }

  private def onHeartbeat(leaderInfo: LeaderInfo) = {
    updateLastLeader(leaderInfo)
    restartHeartbeatTimer()
  }

  private def onAppendNewLogItem(leaderInfo: LeaderInfo,
                                 previousLogItem: Option[LastLogItem],
                                 newLogItem: String,
                                 leaderCommit: Int,
                                 logItemUuid: String): Unit = {
    //TODO Page 11
    //TODO To prevent this problem, servers disregard RequestVote RPCs when they believe a current leader exists.
    //TODO update, only if leader's term is higher?
    if (leaderInfo.term < getLastSeenTerm()) {
      return
    }

    updateLastLeader(leaderInfo)

    if (previousItemFromLeaderLogEqualsLastLogItem(previousLogItem)) {
      log += LogItem(leaderInfo.term, newLogItem)
      applyToSimpleStateMachine(log(leaderCommit))
      leaderInfo.leader ! AppendEntriesResponse(success = true, logItemUuid, followerNodeId)
    } else {
      log = log.init
      leaderInfo.leader ! AppendEntriesResponse(success = false, logItemUuid, followerNodeId)
    }
  }

  private def onRequestVote(candidateTerm: Int, candidate: ActorRef[Command], lastLogItem: Option[LastLogItem]) = {
    val lastLogItemsAreEqual = lastLogItem match {
      case item => previousItemFromLeaderLogEqualsLastLogItem(item)
      case None => true
    }

    //TODO
    //TODO
    //TODO
    //TODO
    //TODO
    //TODO
    //TODO
    //TODO
    //TODO
    //TODO
    //TODO
    if (candidateTerm >= getLastSeenTerm() && lastLogItemsAreEqual && grantedVote.isEmpty) {
      grantedVote = Some(candidateTerm)
      candidate ! VoteGranted
    }
  }

  private def onHeartbeatTimerElapsed(): Behavior[BaseCommand] = {
    Candidate(followerNodeId, log, followerCluster)
  }

  private def restartHeartbeatTimer() = {
    timer.cancelAll()
    timer.startSingleTimer(HeartbeatTimerElapsed, HeartbeatTimerElapsed, heartBeatTimeout)
  }

  private def updateLastLeader(leaderInfo: LeaderInfo) = {
    lastLeader = Some(leaderInfo)
  }

  private def previousItemFromLeaderLogEqualsLastLogItem(previousLogItem: Option[LastLogItem]) = log match {
    case ArrayBuffer(i, _*) if previousLogItem.isDefined =>
      previousLogItem.get.leaderTerm == log.last.leaderTerm && previousLogItem.get.index == log.length - 1
    case _ => true
  }

  private def getLastSeenTerm() = lastLeader match {
    case Some(LeaderInfo(term, _)) => term
    case None => Leader.INITIAL_TERM
  }
}