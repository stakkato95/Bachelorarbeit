package com.stakkato95.raft

import java.util.concurrent.TimeUnit

import akka.actor.typed.ActorSystem

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {
  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem(ActorSystemGuardian(), "guardian")
    actorSystem ! "start"

    println("system started")
    Await.result(actorSystem.whenTerminated, Duration(20, TimeUnit.MINUTES))
  }
}
