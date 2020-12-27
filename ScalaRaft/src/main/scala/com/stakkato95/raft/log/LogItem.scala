package com.stakkato95.raft.log

case class LogItem(leaderTerm: Int, value: String)
