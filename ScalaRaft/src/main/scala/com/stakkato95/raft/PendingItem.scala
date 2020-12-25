package com.stakkato95.raft

case class PendingItem(logItem: LogItem, votes: Int)
