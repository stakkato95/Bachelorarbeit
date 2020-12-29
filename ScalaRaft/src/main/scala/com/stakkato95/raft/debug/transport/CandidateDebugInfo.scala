package com.stakkato95.raft.debug.transport

import com.stakkato95.raft.debug.DebugFiniteDuration

case class CandidateDebugInfo(nodeId: String, term: Option[Int], votes: Int, electionTimeout: DebugFiniteDuration)
