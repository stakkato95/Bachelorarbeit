package com.stakkato95.raft.debug

import com.stakkato95.raft.log.LogItem

case class LogDebugInfo(log: List[LogItem], stateMachineValue: String)
