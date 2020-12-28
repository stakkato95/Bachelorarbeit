package com.stakkato95.raft

import akka.actor.typed.ActorSystem
import com.stakkato95.raft.behavior.RaftClient
import com.stakkato95.raft.behavior.RaftClient.{ClientRequest, ClientStart}
import com.stakkato95.raft.behavior.base.BaseRaftBehavior.Debug
import com.stakkato95.raft.concurrent.ReentrantPromise
import com.stakkato95.raft.log.LogItem

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object Main {

  def main(args: Array[String]): Unit = {
    val promise = new ReentrantPromise[AnyRef]()
    val future = promise.future

    val actorSystem = ActorSystem(RaftClient(promise), "client")
    actorSystem ! ClientStart
    Thread.sleep(10000)

    actorSystem ! ClientRequest("a", actorSystem.ref)
    println(">>>" + future.get[String]())

    actorSystem ! ClientRequest("b", actorSystem.ref)
    println(">>>" + future.get[String]())

    actorSystem ! ClientRequest("c", actorSystem.ref)
    println(">>>" + future.get[String]())

    Thread.sleep(3000)

    actorSystem ! Debug.LogRequest("node-1", actorSystem.ref)
    val info1 = future.get[NodeInfo]()
    println(">>>" + info1)

    actorSystem ! Debug.LogRequest("node-2", actorSystem.ref)
    val info2 = future.get[NodeInfo]()
    println(">>>" + info2)

    actorSystem ! Debug.LogRequest("node-3", actorSystem.ref)
    val info3 = future.get[NodeInfo]()
    println(">>>" + info3)
    

    println("system started")
    Await.result(actorSystem.whenTerminated, 20 minutes)
  }
}
