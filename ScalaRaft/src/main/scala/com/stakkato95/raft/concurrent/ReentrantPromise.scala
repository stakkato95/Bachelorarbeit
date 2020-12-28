package com.stakkato95.raft.concurrent

class ReentrantPromise[T] {

  private val lock = AnyRef
  private val f: RaftFuture = new RaftFuture(lock)

  def success(value: AnyRef): Unit = {
    lock.synchronized {
      f.set(value)
      lock.notify()
    }
  }

  def future: ReentrantFuture = {
    f
  }
}
