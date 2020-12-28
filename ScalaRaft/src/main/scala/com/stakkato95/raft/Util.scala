package com.stakkato95.raft

import akka.actor.typed.ActorRef
import com.stakkato95.raft.behavior.base.BaseCommand
import com.stakkato95.raft.debug.DebugFiniteDuration

import scala.concurrent.duration.{FiniteDuration, _}
import scala.language.postfixOps
import scala.util.Random

object Util {

  def getRandomTimeout(from: Int, to: Int): FiniteDuration = {
    val n = from + (new Random().nextFloat * (to - from)).toLong
    n milliseconds
  }

  def toDebugNextIndices(nextIndices: Map[ActorRef[BaseCommand], Int]): Map[String, Int] = {
    nextIndices.map(pair => (pair._1.path.toString, pair._2))
  }

  def toDebugFiniteDuration(finiteDuration: FiniteDuration): DebugFiniteDuration = {
    DebugFiniteDuration(finiteDuration._1, finiteDuration._2.toString)
  }
}
