package com.stakkato95.raft

import akka.actor.typed.ActorSystem
import com.stakkato95.raft.behavior.{Candidate, Client, Follower, Leader}
import com.stakkato95.raft.behavior.Client.{ClientRequest, ClientStart}
import com.stakkato95.raft.behavior.base.BaseCommand
import com.stakkato95.raft.behavior.base.BaseRaftBehavior.Debug
import com.stakkato95.raft.concurrent.{ReentrantFuture, ReentrantPromise}
import com.stakkato95.raft.debug.LogDebugInfo
import com.stakkato95.raft.debug.transport.{FollowerDebugInfo, LeaderDebugInfo}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object Main {

  def main(args: Array[String]): Unit = {
    val promise = new ReentrantPromise[AnyRef]()
    val future = promise.future

    val actorSystem = ActorSystem(Client(promise), "client")
    actorSystem ! ClientStart

    Thread.sleep(10000)

    workflow1(actorSystem, future)

    println("system started")
    Await.result(actorSystem.whenTerminated, 20 minutes)
  }

  def workflow1(actorSystem: ActorSystem[BaseCommand], future: ReentrantFuture): Unit = {
    actorSystem ! ClientRequest("a", actorSystem.ref)
    println(">>>" + future.get[String]())

    actorSystem ! ClientRequest("b", actorSystem.ref)
    println(">>>" + future.get[String]())

    actorSystem ! ClientRequest("c", actorSystem.ref)
    println(">>>" + future.get[String]())

    Thread.sleep(3000)

    actorSystem ! Debug.InfoRequest("node-1", actorSystem.ref)
    val info1 = future.get[LogDebugInfo]()
    println(">>>" + info1)

    actorSystem ! Debug.InfoRequest("node-2", actorSystem.ref)
    val info2 = future.get[LogDebugInfo]()
    println(">>>" + info2)

    actorSystem ! Debug.InfoRequest("node-3", actorSystem.ref)
    val info3 = future.get[LogDebugInfo]()
    println(">>>" + info3)


    actorSystem ! Leader.Debug.InfoRequest(actorSystem.ref)
    val leaderInfo = future.get[LeaderDebugInfo]()
    println(">>>" + leaderInfo)

    actorSystem ! Follower.Debug.InfoRequest("node-1", actorSystem.ref)
    val followerInfo1 = future.getWithTimeout[FollowerDebugInfo](1000)
    println(">>>" + followerInfo1)

    actorSystem ! Follower.Debug.InfoRequest("node-2", actorSystem.ref)
    val followerInfo2 = future.getWithTimeout[FollowerDebugInfo](1000)
    println(">>>" + followerInfo2)

    actorSystem ! Follower.Debug.InfoRequest("node-3", actorSystem.ref)
    val followerInfo3 = future.getWithTimeout[FollowerDebugInfo](1000)
    println(">>>" + followerInfo3)
  }
}
