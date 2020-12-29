package com.stakkato95.raft.debug.transport

import com.stakkato95.raft.debug.{DebugFiniteDuration, LogDebugInfo}

case class FollowerDebugInfo(nodeId: String,
                             heartBeatTimeout: DebugFiniteDuration,
                             lastApplied: Option[Int],
                             log: LogDebugInfo)
