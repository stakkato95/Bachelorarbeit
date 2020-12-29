package com.stakkato95.raft.debug

import com.stakkato95.raft.log.LogItem

import scala.collection.mutable.ArrayBuffer

case class LogDebugInfo(log: ArrayBuffer[LogItem], stateMachineValue: String)
