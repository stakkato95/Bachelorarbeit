package com.stakkato95.raft.debug

case class LeaderDebugInfo(nodeId: String,
                           nextIndices: Map[String, Int],
                           pendingItems: Map[String, DebugPendingItem],
                           leaderCommit: Option[Int])
