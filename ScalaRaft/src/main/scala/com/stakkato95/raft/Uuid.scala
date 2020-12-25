package com.stakkato95.raft

import java.util.UUID

object Uuid {
  def get: String = UUID.randomUUID().toString
}
