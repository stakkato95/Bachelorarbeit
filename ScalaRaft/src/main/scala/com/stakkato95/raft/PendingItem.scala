package com.stakkato95.raft

case class PendingItem(logItem: LogItem, var votes: Int)
