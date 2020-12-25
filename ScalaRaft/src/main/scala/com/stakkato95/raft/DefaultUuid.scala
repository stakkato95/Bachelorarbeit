package com.stakkato95.raft

import com.stakkato95.raft.uuid.{Uuid, UuidProvider}

class DefaultUuid extends UuidProvider {
  override def get: String = Uuid.get
}
