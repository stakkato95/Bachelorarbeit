package com.stakkato95.raft.behavior

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import com.stakkato95.raft.LeaderInfo
import com.stakkato95.raft.behavior.Candidate.{Command, RequestVote, VoteGranted}
import com.stakkato95.raft.behavior.Follower._
import com.stakkato95.raft.behavior.Leader.AppendEntriesResponse
import com.stakkato95.raft.behavior.RaftClient.ClientRequest
import com.stakkato95.raft.behavior.base.{BaseCommand, BaseRaftBehavior}
import com.stakkato95.raft.log.{LogItem, PreviousLogItem}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration

object Follower {

  def apply(nodeId: String,
            timeout: FiniteDuration,
            log: ArrayBuffer[LogItem],
            cluster: ArrayBuffer[ActorRef[BaseCommand]],
            stateMachineValue: String,
            lastApplied: Option[Int]): Behavior[BaseCommand] = {
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        new Follower(context, nodeId, timers, timeout, log, cluster, stateMachineValue, lastApplied)
      }
    }
  }

  def apply(nodeId: String): Behavior[BaseCommand] = apply(
    nodeId,
    Candidate.ELECTION_TIMEOUT,
    ArrayBuffer[LogItem](),
    ArrayBuffer[ActorRef[BaseCommand]](),
    "",
    None
  )

  trait Command extends BaseCommand

  final case class AppendEntriesHeartbeat(leaderInfo: LeaderInfo, leaderCommit: Option[Int]) extends Command

  final case class AppendEntriesNewLog(leaderInfo: LeaderInfo,
                                       previousLogItem: Option[PreviousLogItem],
                                       newLogItem: LogItem,
                                       leaderCommit: Option[Int],
                                       logItemUuid: Option[String]) extends Command

  private final object HeartbeatTimerElapsed extends Command

  private val NO_LOG_ITEMS_APPLIED = -1
}

class Follower(context: ActorContext[BaseCommand],
               followerNodeId: String,
               timer: TimerScheduler[BaseCommand],
               heartBeatTimeout: FiniteDuration,
               followerLog: ArrayBuffer[LogItem],
               followerCluster: ArrayBuffer[ActorRef[BaseCommand]],
               stateMachineValue: String,
               var lastApplied: Option[Int])
  extends BaseRaftBehavior[BaseCommand](
    context,
    followerNodeId,
    followerLog,
    followerCluster,
    stateMachineValue) {

  context.log.info("{} is follower, timeout = {}", followerNodeId, heartBeatTimeout)
  restartHeartbeatTimer()

  //TODO reset to "None" when new leader established with AppendEntriesHeartbeat
  private var grantedVote: Option[Int] = None

  override def onMessage(msg: BaseCommand): Behavior[BaseCommand] = {
    msg match {
      case AppendEntriesHeartbeat(leaderInfo, leaderCommit) =>
        onHeartbeat(leaderInfo, leaderCommit)
        context.log.info("{} follower receives heartbeat", followerNodeId)
        this
      case AppendEntriesNewLog(leaderInfo, previousLogItem, newLogItem, leaderCommit, logItemUuid) =>
        onAppendNewLogItem(leaderInfo, previousLogItem, newLogItem, leaderCommit, logItemUuid)
        this
      case RequestVote(candidateTerm, candidate, lastLogItem) =>
        onRequestVote(candidateTerm, candidate, lastLogItem)
        this
      case HeartbeatTimerElapsed =>
        onHeartbeatTimerElapsed()
      case request@ClientRequest(_, _) =>
        onClientRequest(request)
        this
      case _ =>
        super.onMessage(msg)
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[BaseCommand]] = {
    case PostStop =>
      context.log.info("{} stop", followerNodeId)
      this
  }

  private def onHeartbeat(leaderInfo: LeaderInfo, leaderCommit: Option[Int]) = {
    if (leaderInfo.term >= getLastSeenTerm()) {
      updateLastLeader(leaderInfo)
      commitLog(leaderCommit)
    }

    restartHeartbeatTimer()
  }

  private def onAppendNewLogItem(leaderInfo: LeaderInfo,
                                 previousLogItem: Option[PreviousLogItem],
                                 newLogItem: LogItem,
                                 leaderCommit: Option[Int],
                                 logItemUuid: Option[String]): Unit = {
    //TODO Page 11
    //TODO To prevent this problem, servers disregard RequestVote RPCs when they believe a current leader exists.
    //TODO update, only if leader's term is higher?
    if (leaderInfo.term < getLastSeenTerm()) {
      return
    }

    updateLastLeader(leaderInfo)


    if (log.isEmpty) {
      previousLogItem match {
        case None =>
          //start of the log is reached, so confirm first element to Leader
          applyLogItem(leaderInfo, newLogItem, logItemUuid)
        case _ =>
          //reach the very beginning of the log at Leader and force it to resend log starting from the first item
          leaderInfo.leader ! AppendEntriesResponse(success = false, logItemUuid, nodeId, context.self)

          //last item can not be applied, since it is still invalid / inconsistent with Leader's log
          return
      }
    } else {
      if (previousItemFromLeaderLogEqualsLastLogItem(previousLogItem)) {
        applyLogItem(leaderInfo, newLogItem, logItemUuid)
      } else {
        lastApplied = lastApplied.map(_ - 1)
        log.remove(log.size - 1)
        unapplyFromSimpleStateMachine()
        leaderInfo.leader ! AppendEntriesResponse(success = false, logItemUuid, nodeId, context.self)

        //last item can not be applied, since it is still invalid / inconsistent with Leader's log
        return
      }
    }

    commitLog(leaderCommit)
  }

  private def onRequestVote(candidateTerm: Int, candidate: ActorRef[Command], lastLogItem: Option[PreviousLogItem]) = {
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
    Candidate(followerNodeId, log, followerCluster, currentStateMachineValue)
  }

  private def onClientRequest(request: ClientRequest): Unit = {
    lastLeader match {
      case Some(LeaderInfo(_, leader)) =>
        leader ! request
      case None =>
        context.log.info("{}: no leader for passing ClientRequest", nodeId)
    }
  }

  private def restartHeartbeatTimer() = {
    timer.cancelAll()
    timer.startSingleTimer(HeartbeatTimerElapsed, HeartbeatTimerElapsed, heartBeatTimeout)
  }

  private def updateLastLeader(leaderInfo: LeaderInfo) = {
    lastLeader = Some(leaderInfo)
  }

  private def previousItemFromLeaderLogEqualsLastLogItem(previousLogItem: Option[PreviousLogItem]) = log match {
    case ArrayBuffer(i, _*) if previousLogItem.isDefined =>
      previousLogItem.get.leaderTerm == log.last.leaderTerm && previousLogItem.get.index == log.length - 1
    case _ => true
  }

  private def getLastSeenTerm() = lastLeader match {
    case Some(LeaderInfo(term, _)) => term
    case None => Leader.INITIAL_TERM
  }

  private def applyLogItem(leaderInfo: LeaderInfo,
                           newLogItem: LogItem,
                           logItemUuid: Option[String]): Unit = {
    log += newLogItem
    leaderInfo.leader ! AppendEntriesResponse(success = true, logItemUuid, nodeId, context.self)
  }

  private def commitLog(leaderCommit: Option[Int]): Unit = {
    val followerCommit = lastApplied match {
      case Some(commit) => commit
      case None => Follower.NO_LOG_ITEMS_APPLIED
    }

    leaderCommit match {
      case Some(commit) if commit > followerCommit && log.nonEmpty =>
        // assume follower's log can lay only 1 item behind leaders log
        lastApplied = Some(commit)
        applyToSimpleStateMachine(log(commit))
      case None | _ =>
      // nothing to commit yet. Most probably the very first item received to log
    }
  }
}