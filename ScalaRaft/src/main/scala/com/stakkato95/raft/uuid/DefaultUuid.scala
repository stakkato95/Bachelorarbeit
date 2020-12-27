package com.stakkato95.raft.uuid

class DefaultUuid extends UuidProvider {
  override def get: String = Uuid.get
}
