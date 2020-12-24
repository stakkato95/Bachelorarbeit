package com.stakkato95.raft.behavior

import java.util.concurrent.TimeUnit

import akka.actor.typed.{ActorRef, Behavior, Signal}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import com.stakkato95.raft.LogItem
import com.stakkato95.raft.behavior.Follower.AppendEntriesHeartbeat

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

  //TODO timers to repeat sending of log items, which have not been confirmed

  override def onMessage(msg: BaseCommand): Behavior[BaseCommand] = super.onMessage(msg)

  override def onSignal: PartialFunction[Signal, Behavior[BaseCommand]] = super.onSignal

  private def establishLeadership() = {
    getRestOfCluster().foreach(_ ! AppendEntriesHeartbeat(leaderTerm, context.self))
  }
}
