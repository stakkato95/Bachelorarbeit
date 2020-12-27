package com.stakkato95.raft

import akka.actor.typed.ActorSystem
import com.stakkato95.raft.behavior.RaftClient
import com.stakkato95.raft.behavior.RaftClient.{ClientRequest, ClientStart}
import com.stakkato95.raft.concurrent.ReentrantPromise

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object Main {

  def main(args: Array[String]): Unit = {
    val promise = new ReentrantPromise[String]()
    val future = promise.future

    val actorSystem = ActorSystem(RaftClient(promise), "client")
    actorSystem ! ClientStart
    Thread.sleep(10000)

    actorSystem ! ClientRequest("a", actorSystem.ref)
    println(">>>" + future.get())

    actorSystem ! ClientRequest("b", actorSystem.ref)
    println(">>>" + future.get())

    actorSystem ! ClientRequest("c", actorSystem.ref)
    println(">>>" + future.get())

    println("system started")
    Await.result(actorSystem.whenTerminated, 20 minutes)
  }
}
