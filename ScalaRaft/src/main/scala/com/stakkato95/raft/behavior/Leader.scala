package com.stakkato95.raft.behavior

import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, Signal}
import com.stakkato95.raft.LogItem

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration

object Leader {

  def apply(nodeId: String,
            timeout: FiniteDuration,
            log: ArrayBuffer[LogItem],
            cluster: ArrayBuffer[ActorRef[BaseCommand]],
            leaderTerm: Int): Behavior[BaseCommand] = {
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        new Leader(context, nodeId, timers, timeout, log, cluster, leaderTerm)
      }
    }
  }

  def apply(nodeId: String,
            log: ArrayBuffer[LogItem],
            cluster: ArrayBuffer[ActorRef[BaseCommand]],
            leaderTerm: Int): Behavior[BaseCommand] = {
    apply(nodeId, RESEND_LOG_TIMEOUT, log, cluster, leaderTerm)
  }

  trait Command extends BaseCommand

  final case class AppendEntriesResponse(success: Boolean, logItemUuid: String, nodeId: String) extends Command

  val INITIAL_TERM = 0

  private val RESEND_LOG_TIMEOUT = FiniteDuration(1, TimeUnit.SECONDS)
}


class Leader(context: ActorContext[BaseCommand],
             nodeId: String,
             timers: TimerScheduler[BaseCommand],
             timeout: FiniteDuration,
             log: ArrayBuffer[LogItem],
             cluster: ArrayBuffer[ActorRef[BaseCommand]],
             leaderTerm: Int) extends BaseRaftBehavior[BaseCommand](context, nodeId, log, cluster) {

  establishLeadership()

  //index of the next log entry the leader will send to that follower.

  //When a leader first comes to power, it initializes all nextIndex
  // values to the index just after the last one in its log

  //Af- ter a rejection, the leader decrements nextIndex and retries the AppendEntries RPC.
  //Eventually nextIndex will reach a point where the leader and follower logs match.
  // When this happens, AppendEntries will succeed, which removes any conflicting entries
  // in the follower’s log and appends entries from the leader’s log (if any).
  // Once AppendEntries succeeds, the follower’s log is consistent with the leader’s,
  // and it will remain that way for the rest of the term.
  private var nextIndices = Map[String, Int]()

  //TODO timers to repeat sending of log items, which have not been confirmed

  override def onMessage(msg: BaseCommand): Behavior[BaseCommand] = super.onMessage(msg)

  override def onSignal: PartialFunction[Signal, Behavior[BaseCommand]] = super.onSignal

  private def establishLeadership() = {
//    getRestOfCluster().foreach(_ ! AppendEntriesHeartbeat(leaderTerm, context.self))
  }
}
