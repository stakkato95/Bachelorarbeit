package com.stakkato95.raft.debug

import scala.concurrent.duration.FiniteDuration

case class FollowerDebugInfo(nodeId: String, heartBeatTimeout: FiniteDuration)
