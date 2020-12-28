package com.stakkato95.raft.debug

import scala.concurrent.duration.FiniteDuration

case class CandidateDebugInfo(nodeId: String, term: Option[Int], votes: Int, electionTimeout: FiniteDuration)
