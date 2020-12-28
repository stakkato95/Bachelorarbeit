package com.stakkato95.raft.debug

import akka.actor.typed.ActorRef
import com.stakkato95.raft.behavior.base.BaseCommand
import com.stakkato95.raft.log.PendingItem

case class LeaderDebugInfo(nodeId: String,
                           nextIndices: Map[ActorRef[BaseCommand], Int],
                           pendingItems: Map[String, PendingItem],
                           leaderCommit: Option[Int])
