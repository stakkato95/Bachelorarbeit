package com.stakkato95.raft

import java.util.concurrent.TimeUnit

import akka.actor.typed.ActorSystem
import com.stakkato95.raft.RaftClient.{ClientRequest, ClientStart}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem(RaftClient(clientCallback), "client")
    actorSystem ! ClientStart
    Thread.sleep(10000)

    actorSystem ! ClientRequest("a", actorSystem.ref)
    Thread.sleep(10000)


    //TODO rename ActorSystemGuardian to RaftClient
    //TODO pass client request to RaftClient
    //TODO route from leader to follower
    //TODO send msg to system guardian an then to ONE OF NODES => Round Robin

    println("system started")
    Await.result(actorSystem.whenTerminated, Duration(20, TimeUnit.MINUTES))
  }

  def clientCallback(value: String): Unit = {
    println(value)
  }
}
