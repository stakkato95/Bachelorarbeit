package com.stakkato95.raft.behavior

import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, Signal}
import com.stakkato95.raft.behavior.Follower.{AppendEntriesHeartbeat, AppendEntriesNewLog}
import com.stakkato95.raft.behavior.Leader.{AppendEntriesResponse, ClientRequest, ClientResponse}
import com.stakkato95.raft.behavior.base.{BaseCommand, BaseRaftBehavior}
import com.stakkato95.raft.uuid.UuidProvider
import com.stakkato95.raft.{DefaultUuid, PreviousLogItem, LeaderInfo, LogItem, PendingItem}

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
    apply(nodeId, RESEND_LOG_TIMEOUT, log, cluster, leaderTerm, new DefaultUuid, stateMachineValue)
  }

  trait Command extends BaseCommand

  final case class AppendEntriesResponse(success: Boolean,
                                         logItemUuid: Option[String],
                                         nodeId: String,
                                         replyTo: ActorRef[BaseCommand]) extends Command

  final case class ClientRequest(value: String, replyTo: ActorRef[ClientResponse]) extends Command

  final case class ClientResponse(currentState: String)

  val INITIAL_TERM = 0

  private val RESEND_LOG_TIMEOUT = FiniteDuration(1, TimeUnit.SECONDS)
}


class Leader(context: ActorContext[BaseCommand],
             leaderNodeId: String,
             timers: TimerScheduler[BaseCommand],
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

  context.log.info("{} is follower", nodeId)
  establishLeadership()

  //TODO timers to repeat sending of log items, which have not been confirmed

  override def onMessage(msg: BaseCommand): Behavior[BaseCommand] = {
    msg match {
      case ClientRequest(value, replyTo) =>
        onClientRequest(value, replyTo)
        this
      case AppendEntriesResponse(success, logItemUuid, nodeId, replyTo) =>
        onAppendEntriesResponse(success, logItemUuid, nodeId, replyTo)
        this
      case _ =>
        super.onMessage(msg)
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[BaseCommand]] = super.onSignal

  private def establishLeadership() = {
    getRestOfCluster().foreach(_ ! AppendEntriesHeartbeat(LeaderInfo(leaderTerm, context.self), leaderCommit))
  }

  private def onClientRequest(value: String, replyTo: ActorRef[ClientResponse]) = {
    val uuid = uuidProvider.get
    val logItem = LogItem(leaderTerm, value)
    pendingItems += uuid -> PendingItem(logItem, 1, replyTo)

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
