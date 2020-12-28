package com.stakkato95.raft.debug

case class CandidateDebugInfo(nodeId: String, term: Option[Int], votes: Int, electionTimeout: DebugFiniteDuration)
