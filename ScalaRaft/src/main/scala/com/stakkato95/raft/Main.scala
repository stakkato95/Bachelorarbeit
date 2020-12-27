package com.stakkato95.raft

import java.util.concurrent.TimeUnit

import akka.actor.typed.ActorSystem
import com.stakkato95.raft.behavior.RaftClient
import com.stakkato95.raft.behavior.RaftClient.{ClientRequest, ClientStart}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem(RaftClient(clientCallback), "client")
    actorSystem ! ClientStart
    Thread.sleep(10000)

    val timer = 5000
    actorSystem ! ClientRequest("a", actorSystem.ref)
    Thread.sleep(timer)
    actorSystem ! ClientRequest("b", actorSystem.ref)
    Thread.sleep(timer)
    actorSystem ! ClientRequest("c", actorSystem.ref)
    Thread.sleep(timer)
//    actorSystem ! ClientRequest("d", actorSystem.ref)
//    Thread.sleep(timer)
//    actorSystem ! ClientRequest("e", actorSystem.ref)
//    Thread.sleep(timer)
//    actorSystem ! ClientRequest("f", actorSystem.ref)
//    Thread.sleep(timer)


//    Thread.sleep(10000)

    println("system started")
    Await.result(actorSystem.whenTerminated, Duration(20, TimeUnit.MINUTES))
  }

  def clientCallback(value: String): Unit = {
    println("===" + value)
  }
}
