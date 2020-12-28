package com.stakkato95.raft

import akka.actor.typed.ActorRef
import com.stakkato95.raft.behavior.base.BaseCommand

case class ClusterItem(nodeId: String, ref: ActorRef[BaseCommand])
