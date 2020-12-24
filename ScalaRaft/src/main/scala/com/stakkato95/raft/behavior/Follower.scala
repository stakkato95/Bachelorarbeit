package com.stakkato95.raft.behavior

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import com.stakkato95.raft.behavior.Candidate.RequestVote
import com.stakkato95.raft.behavior.Follower._
import com.stakkato95.raft.behavior.Leader.AppendEntriesResponse
import com.stakkato95.raft.{LastLogItem, LeaderInfo, LogItem}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration

object Follower {

  def apply(nodeId: String,
            timeout: FiniteDuration,
            log: ArrayBuffer[LogItem],
            cluster: ArrayBuffer[ActorRef[BaseCommand]]): Behavior[BaseCommand] =
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        new Follower(context, nodeId, timers, timeout, log, cluster)
      }
    }

  trait Command extends BaseCommand

  case class AppendEntriesHeartbeat(leaderTerm: Int, leader: ActorRef[Leader.Command]) extends Command

  case class AppendEntriesNewLog(leaderTerm: Int,
                                 leader: ActorRef[Leader.Command],
                                 previousLogItem: LastLogItem,
                                 newLogItem: String,
                                 leaderCommit: Int,
                                 logItemUuid: String) extends Command

  private object HeartbeatTimerElapsed extends Command

}

class Follower(context: ActorContext[BaseCommand],
               nodeId: String,
               timers: TimerScheduler[BaseCommand],
               timeout: FiniteDuration,
               log: ArrayBuffer[LogItem],
               cluster: ArrayBuffer[ActorRef[BaseCommand]])
  extends BaseRaftBehavior[BaseCommand](context, nodeId, log, cluster) {

  context.log.info("{} is follower", nodeId)
  restartHeartbeatTimer()

  override def onMessage(msg: BaseCommand): Behavior[BaseCommand] = {
    msg match {
      case AppendEntriesHeartbeat(leaderTerm, leader) =>
        onHeartbeat(leaderTerm, leader)
        this
      case AppendEntriesNewLog(leaderTerm, leader, previousLogItem, newLogItem, leaderCommit, logItemUuid) =>
        onAppendNewLogItem(leaderTerm, leader, previousLogItem, newLogItem, leaderCommit, logItemUuid)
        this
      case RequestVote(candidateTerm, candidate, lastLogItem) =>
        this
      case HeartbeatTimerElapsed =>
        onHeartbeatTimerElapsed()
      case _ =>
        super.onMessage(msg)
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[BaseCommand]] = {
    case PostStop =>
      context.log.info("{} stop", nodeId)
      this
  }

  private def onHeartbeat(leaderTerm: Int, leader: ActorRef[Leader.Command]) = {
    updateLastLeader(leaderTerm, leader)
    restartHeartbeatTimer()
  }

  private def onAppendNewLogItem(leaderTerm: Int,
                                 leader: ActorRef[Leader.Command],
                                 previousLogItem: LastLogItem,
                                 newLogItem: String,
                                 leaderCommit: Int,
                                 logItemUuid: String) = {
    updateLastLeader(leaderTerm, leader)

    if (previousItemFromLeaderLogEqualsLastLogItem(previousLogItem)) {
      log += LogItem(leaderTerm, newLogItem)
      applyToSimpleStateMachine(log(leaderCommit))
      leader ! AppendEntriesResponse(success = true, logItemUuid, nodeId)
    } else {
      leader ! AppendEntriesResponse(success = false, logItemUuid, nodeId)
    }
  }

  private def onHeartbeatTimerElapsed(): Behavior[BaseCommand] = {
    Candidate(nodeId, log, cluster)
  }

  private def restartHeartbeatTimer() = {
    timers.cancelAll()
    timers.startSingleTimer(HeartbeatTimerElapsed, HeartbeatTimerElapsed, timeout)
  }

  private def updateLastLeader(leaderTerm: Int, leader: ActorRef[Leader.Command]) = {
    lastLeader = Some(LeaderInfo(leaderTerm, leader))
  }

  private def previousItemFromLeaderLogEqualsLastLogItem(previousLogItem: LastLogItem) = log match {
    case ArrayBuffer(i, _*) => previousLogItem.leaderTerm == log.last.leaderTerm && previousLogItem.index == log.length - 1
    case _ => true
  }
}