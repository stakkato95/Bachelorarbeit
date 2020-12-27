package com.stakkato95.raft.behavior

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, Signal}
import com.stakkato95.raft
import com.stakkato95.raft.RaftClient.{ClientRequest, ClientResponse}
import com.stakkato95.raft.behavior.Follower.{AppendEntriesHeartbeat, AppendEntriesNewLog}
import com.stakkato95.raft.behavior.Leader.{AppendEntriesResponse, LeaderTimerElapsed}
import com.stakkato95.raft.behavior.base.{BaseCommand, BaseRaftBehavior}
import com.stakkato95.raft.log.{LogItem, PendingItem, PreviousLogItem}
import com.stakkato95.raft.uuid.{DefaultUuid, UuidProvider}
import com.stakkato95.raft.{LeaderInfo, Util}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration

object Leader {

  def apply(nodeId: String,
            timeout: FiniteDuration,
            log: ArrayBuffer[LogItem],
            cluster: ArrayBuffer[ActorRef[BaseCommand]],
            leaderTerm: Int,
            uuidProvider: UuidProvider,
            stateMachineValue: String): Behavior[BaseCommand] = {
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        new Leader(context, nodeId, timers, timeout, log, cluster, leaderTerm, uuidProvider, stateMachineValue)
      }
    }
  }

  def apply(nodeId: String,
            log: ArrayBuffer[LogItem],
            cluster: ArrayBuffer[ActorRef[BaseCommand]],
            leaderTerm: Int,
            stateMachineValue: String): Behavior[BaseCommand] = {
    apply(nodeId, LEADER_TIMEOUT, log, cluster, leaderTerm, new DefaultUuid, stateMachineValue)
  }

  trait Command extends BaseCommand

  final case class AppendEntriesResponse(success: Boolean,
                                         logItemUuid: Option[String],
                                         nodeId: String,
                                         replyTo: ActorRef[BaseCommand]) extends Command

  private final object LeaderTimerElapsed extends Command

  val INITIAL_TERM = 0

  val MIN_ELECTION_TIMEOUT = 1
  val MAX_ELECTION_TIMEOUT = 2

  def LEADER_TIMEOUT: FiniteDuration = Util.getRandomTimeout(MIN_ELECTION_TIMEOUT, MAX_ELECTION_TIMEOUT)
}


class Leader(context: ActorContext[BaseCommand],
             leaderNodeId: String,
             timer: TimerScheduler[BaseCommand],
             timeout: FiniteDuration,
             leaderLog: ArrayBuffer[LogItem],
             leaderCluster: ArrayBuffer[ActorRef[BaseCommand]],
             leaderTerm: Int,
             uuidProvider: UuidProvider,
             stateMachineValue: String)
  extends BaseRaftBehavior[BaseCommand](
    context,
    leaderNodeId,
    leaderLog,
    leaderCluster,
    stateMachineValue) {

  //TODO logic how Leader can again become Follower

  //index of the next log entry the leader will send to that follower.

  //When a leader first comes to power, it initializes all nextIndex
  // values to the index just after the last one in its log

  //Af- ter a rejection, the leader decrements nextIndex and retries the AppendEntries RPC.
  //Eventually nextIndex will reach a point where the leader and follower logs match.
  // When this happens, AppendEntries will succeed, which removes any conflicting entries
  // in the follower’s log and appends entries from the leader’s log (if any).
  // Once AppendEntries succeeds, the follower’s log is consistent with the leader’s,
  // and it will remain that way for the rest of the term.
  private var nextIndices: Map[ActorRef[BaseCommand], Int] = cluster.map((_, log.size)).toMap
  private var pendingItems = Map[String, PendingItem]()
  private var leaderCommit: Option[Int] = if (log.nonEmpty) Some(log.size - 1) else None

  context.log.info("{} is leader", nodeId)
  onLeadershipTimerElapsed()

  //TODO timers to repeat sending of log items, which have not been confirmed

  override def onMessage(msg: BaseCommand): Behavior[BaseCommand] = {
    msg match {
      case ClientRequest(value, replyTo) =>
        onClientRequest(value, replyTo)
        this
      case AppendEntriesResponse(success, logItemUuid, nodeId, replyTo) =>
        onAppendEntriesResponse(success, logItemUuid, nodeId, replyTo)
        this
      case LeaderTimerElapsed =>
        onLeadershipTimerElapsed()
        this
      case _ =>
        super.onMessage(msg)
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[BaseCommand]] = super.onSignal

  private def establishLeadership() = {
    getRestOfCluster().foreach(_ ! AppendEntriesHeartbeat(LeaderInfo(leaderTerm, context.self), leaderCommit))
  }

  private def startLeadershipTimer(): Unit = {
    timer.startSingleTimer(LeaderTimerElapsed, LeaderTimerElapsed, timeout)
  }

  private def onLeadershipTimerElapsed(): Unit = {
    establishLeadership()
    startLeadershipTimer()
  }

  private def onClientRequest(value: String, replyTo: ActorRef[ClientResponse]) = {
    val uuid = uuidProvider.get
    val logItem = LogItem(leaderTerm, value)
    pendingItems += uuid -> raft.log.PendingItem(logItem, 1, replyTo)

    val msg = AppendEntriesNewLog(
      leaderInfo = LeaderInfo(leaderTerm, context.self),
      previousLogItem = previousLogItem,
      newLogItem = logItem,
      leaderCommit = leaderCommit,
      logItemUuid = Some(uuid)
    )

    log += logItem
    getRestOfCluster().foreach(_ ! msg)
  }

  private def onAppendEntriesResponse(success: Boolean,
                                      logItemUuid: Option[String],
                                      nodeId: String,
                                      replyTo: ActorRef[BaseCommand]): Unit = {
    if (success) {
      logItemUuid match {
        case Some(uuid) =>
          onAppendEntriesResponseSuccess(uuid, nodeId, replyTo)
        case None =>
          onAppendEntriesRetrySuccess(nodeId, replyTo)
      }
    } else {
      onAppendEntriesResponseFailure(replyTo)
    }
  }

  private def onAppendEntriesResponseSuccess(logItemUuid: String,
                                             nodeId: String,
                                             replyTo: ActorRef[BaseCommand]) = {
    val nextIndexForFollower = nextIndices(replyTo) + 1
    nextIndices += replyTo -> nextIndexForFollower

    pendingItems(logItemUuid).votes += 1

    if (pendingItems(logItemUuid).votes >= quorumSize) {
      val pendingItem = pendingItems(logItemUuid)
      pendingItems -= logItemUuid
      applyToSimpleStateMachine(pendingItem.logItem)
      leaderCommit = leaderCommit.map(_ + 1)

      pendingItem.replyTo ! ClientResponse(currentStateMachineValue)
    }
  }

  private def onAppendEntriesRetrySuccess(nodeId: String, replyTo: ActorRef[BaseCommand]): Unit = {
    if (nextIndices(replyTo) == log.size - 1) {
      return
    }

    val nextIndexForFollower = nextIndices(replyTo) + 1
    nextIndices += replyTo -> nextIndexForFollower

    sendCorrectingAppendEntries(replyTo, nextIndexForFollower)
  }

  private def onAppendEntriesResponseFailure(replyTo: ActorRef[BaseCommand]) = {
    val nextIndexForFollower = nextIndices(replyTo) - 1
    nextIndices += replyTo -> nextIndexForFollower

    sendCorrectingAppendEntries(replyTo, nextIndexForFollower)
  }

  private def sendCorrectingAppendEntries(replyTo: ActorRef[BaseCommand], nextIndexForFollower: Int): Unit = {
    //previousIndex < nextIndexForFollower
    val previousIndex = nextIndexForFollower - 1
    replyTo ! AppendEntriesNewLog(
      leaderInfo = LeaderInfo(term = leaderTerm, leader = context.self),
      previousLogItem = Some(PreviousLogItem(
        index = previousIndex,
        leaderTerm = log(previousIndex).leaderTerm
      )),
      newLogItem = log(nextIndices(replyTo)),
      leaderCommit = leaderCommit,
      logItemUuid = None //uuid is not important, since success=false is received from one particular node
    )
  }
}
