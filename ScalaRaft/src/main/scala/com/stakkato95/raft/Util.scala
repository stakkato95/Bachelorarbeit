package com.stakkato95.raft

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

object Util {

  def getRandomTimeout(from: Int, to: Int): FiniteDuration = {
    val length = from + (new Random().nextFloat * (to - from)).toLong
    FiniteDuration(length, TimeUnit.SECONDS)
  }
}
