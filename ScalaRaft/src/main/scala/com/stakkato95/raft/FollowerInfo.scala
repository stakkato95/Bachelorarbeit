package com.stakkato95.raft

import scala.concurrent.duration.FiniteDuration

case class FollowerInfo(nodeId: String, heartBeatTimeout: FiniteDuration)
