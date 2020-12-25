package com.stakkato95.raft.uuid

trait UuidProvider {
  def get: String
}
