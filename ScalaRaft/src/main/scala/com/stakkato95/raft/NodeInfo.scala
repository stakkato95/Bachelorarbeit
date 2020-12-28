package com.stakkato95.raft

import com.stakkato95.raft.log.LogItem

case class NodeInfo(log: List[LogItem], stateMachineValue: String)
