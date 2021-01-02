package com.stakkato95.raft

import akka.actor.typed.ActorSystem
import com.stakkato95.raft.behavior.Client.{ClientRequest, ClientStart}
import com.stakkato95.raft.behavior.base.BaseCommand
import com.stakkato95.raft.behavior.{Client, Follower, Leader}
import com.stakkato95.raft.concurrent.{ReentrantFuture, ReentrantPromise}
import com.stakkato95.raft.debug.transport.{CandidateDebugInfo, FollowerDebugInfo, LeaderDebugInfo}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object Main {

  def main(args: Array[String]): Unit = {
    val replicationPromise = new ReentrantPromise[String]()
    val leaderPromise = new ReentrantPromise[LeaderDebugInfo]()
    val followersPromise = new ReentrantPromise[FollowerDebugInfo]()
    val candidatesPromise = new ReentrantPromise[CandidateDebugInfo]()
    val replicationFuture = replicationPromise.future
    val leaderFuture = leaderPromise.future
    val followersFuture = followersPromise.future
    val candidatesFuture = candidatesPromise.future

    val actorSystem = ActorSystem(Client(replicationPromise, leaderPromise, followersPromise, candidatesPromise), "client")
    actorSystem ! ClientStart

    Thread.sleep(10000)

    workflow1(actorSystem, replicationFuture, leaderFuture, followersFuture, candidatesFuture)

    println("system started")
    Await.result(actorSystem.whenTerminated, 20 minutes)
  }

  def workflow1(actorSystem: ActorSystem[BaseCommand],
                replicationFuture: ReentrantFuture[String],
                leaderFuture: ReentrantFuture[LeaderDebugInfo],
                followersFuture: ReentrantFuture[FollowerDebugInfo],
                candidatesFuture: ReentrantFuture[CandidateDebugInfo]): Unit = {
    actorSystem ! ClientRequest("a", actorSystem.ref)
    println(">>>" + replicationFuture.get())

    actorSystem ! ClientRequest("b", actorSystem.ref)
    println(">>>" + replicationFuture.get())

    actorSystem ! ClientRequest("c", actorSystem.ref)
    println(">>>" + replicationFuture.get())

    Thread.sleep(3000)

    //    actorSystem ! Debug.InfoRequest("node-1", actorSystem.ref)
    //    val info1 = future.get[LogDebugInfo]()
    //    println(">>>" + info1)
    //
    //    actorSystem ! Debug.InfoRequest("node-2", actorSystem.ref)
    //    val info2 = future.get[LogDebugInfo]()
    //    println(">>>" + info2)
    //
    //    actorSystem ! Debug.InfoRequest("node-3", actorSystem.ref)
    //    val info3 = future.get[LogDebugInfo]()
    //    println(">>>" + info3)


    actorSystem ! Leader.Debug.InfoRequest(actorSystem.ref)
    val leaderInfo = leaderFuture.get()
    println(">>>" + leaderInfo)

    actorSystem ! Follower.Debug.InfoRequest("node-1", actorSystem.ref)
    val followerInfo1 = followersFuture.getWithTimeout(1000)
    println(">>>" + followerInfo1)

    actorSystem ! Follower.Debug.InfoRequest("node-2", actorSystem.ref)
    val followerInfo2 = followersFuture.getWithTimeout(1000)
    println(">>>" + followerInfo2)

    actorSystem ! Follower.Debug.InfoRequest("node-3", actorSystem.ref)
    val followerInfo3 = followersFuture.getWithTimeout(1000)
    println(">>>" + followerInfo3)
  }
}
