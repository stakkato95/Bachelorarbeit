package com.stakkato95.raft.concurrent

class RaftFuture(lock: AnyRef) extends ReentrantFuture {

  private var result: Option[AnyRef] = None

  def set(value: AnyRef): Unit = {
    result = Some(value)
  }

  def get[U](): U = {
    //TODO blocks thread forever, if ReentrantPromise.success was called once
    //and will never be called in the future. Implemented with awareness of this risk

    lock.synchronized {
      lock.wait()
      result.get.asInstanceOf[U]
    }
  }
}