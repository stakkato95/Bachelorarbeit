package com.stakkato95.raft.concurrent

class RaftFuture(lock: AnyRef) extends ReentrantFuture {

  private var result: Option[AnyRef] = None
  private var isValueSet = false

  def set(value: AnyRef): Unit = {
    isValueSet = true
    result = Some(value)
  }

  def getWithTimeout[U](timeoutMillis: Long): Option[U] = {
    //TODO blocks thread forever, if ReentrantPromise.success was called once
    //and will never be called in the future. Implemented with awareness of this risk

    lock.synchronized {
      lock.wait(timeoutMillis)
      if (isValueSet) {
        isValueSet = false
        Some(result.get.asInstanceOf[U])
      } else {
        None
      }
    }
  }

  def get[U](): Option[U] = {
    getWithTimeout(0)
  }
}