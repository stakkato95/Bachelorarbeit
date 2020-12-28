package com.stakkato95.raft.debug

import com.stakkato95.raft.log.LogItem

case class DebugPendingItem(logItem: LogItem, var votes: Int)