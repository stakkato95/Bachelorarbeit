package services

import akka.actor.typed.ActorSystem
import com.stakkato95.raft.behavior.RaftClient
import com.stakkato95.raft.behavior.RaftClient.{ClientRequest, ClientStart}
import com.stakkato95.raft.concurrent.ReentrantPromise
import javax.inject.Inject
import models.{ClusterItem, ClusterState, Person}

class ClusterService @Inject()() {

  private val promise = new ReentrantPromise[String]()
  private val future = promise.future
  private val actorSystem = ActorSystem(RaftClient(promise), "client")
  actorSystem ! ClientStart

  def showPerson(person: Person): Unit = {
    println(s"success $person")
  }

  def addItemToCluster(item: ClusterItem): ClusterState = {
    actorSystem ! ClientRequest(item.value, actorSystem.ref)
    val clusterState = future.get()
    ClusterState(state = clusterState)
  }
}
