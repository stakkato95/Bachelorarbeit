package com.stakkato95.raft

import scala.concurrent.duration.{FiniteDuration, _}
import scala.language.postfixOps
import scala.util.Random

object Util {

  def getRandomTimeout(from: Int, to: Int): FiniteDuration = {
    val n = from + (new Random().nextFloat * (to - from)).toLong
    n milliseconds
  }
}
