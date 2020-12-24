package com.stakkato95.raft.behavior

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}
import com.stakkato95.raft.{LeaderInfo, LogItem}
import com.stakkato95.raft.behavior.Follower.{AppendEntriesHeartbeat, AppendEntriesNewLog, HeartbeatTimerElapsed, RequestVote}

import scala.concurrent.duration.FiniteDuration

object Follower {

  def apply(id: String, timers: TimerScheduler[Command], timeout: FiniteDuration): Behavior[Command] =
    Behaviors.setup(new Follower(_, id, timers, timeout))


  case class AppendEntriesHeartbeat(leaderTerm: Int, leader: ActorRef[Command]) extends Command

  case class AppendEntriesNewLog(leaderTerm: Int,
                                 leader: ActorRef[Command],
                                 previousLogItem: LogItem,
                                 newLogItem: String,
                                 leaderCommit: Int) extends Command

  case class RequestVote(candidateTerm: Int,
                         candidate: ActorRef[Command],
                         lastLogItem: LogItem) extends Command

  private object HeartbeatTimerElapsed extends Command

}

class Follower(context: ActorContext[Command],
               id: String,
               timers: TimerScheduler[Command],
               timeout: FiniteDuration) extends AbstractBehavior[Command](context) {

  context.log.info("{} is follower", id)
  restartHeartbeatTimer()

  private var lastLeader: Option[LeaderInfo] = None

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case AppendEntriesHeartbeat(leaderTerm, leader) =>
        onHeartbeat(leaderTerm, leader)
        this
      case AppendEntriesNewLog(leaderTerm, leader, previousLogItem, newLogItem, leaderCommit) =>
        this
      case RequestVote(candidateTerm, candidate, lastLogItem) =>
        this
      case HeartbeatTimerElapsed =>
        onHeartbeatTimerElapsed()
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("{} stop", id)
      this
  }

  private def onHeartbeat(leaderTerm: Int, leader: ActorRef[Command]) = {
    lastLeader = Some(LeaderInfo(leaderTerm, leader))
    restartHeartbeatTimer()
  }

  private def onHeartbeatTimerElapsed(): Behavior[Command] = {
    Candidate(id)
  }

  private def restartHeartbeatTimer() = {
    timers.cancelAll()
    timers.startSingleTimer(HeartbeatTimerElapsed, HeartbeatTimerElapsed, timeout)
  }
}
