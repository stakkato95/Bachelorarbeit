package com.stakkato95.raft.debug.transport

import com.stakkato95.raft.debug.{DebugPendingItem, LogDebugInfo}

case class LeaderDebugInfo(nodeId: String,
                           nextIndices: Map[String, Int],
                           pendingItems: Map[String, DebugPendingItem],
                           leaderCommit: Option[Int],
                           log: LogDebugInfo)
