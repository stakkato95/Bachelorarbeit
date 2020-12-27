package com.stakkato95.raft.log

case class PreviousLogItem(index: Int, leaderTerm: Int)
