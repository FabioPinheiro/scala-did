package fmgp.prism

extension (state: PrismState)
  def lastSyncedBlockTimestamp: com.google.protobuf.timestamp.Timestamp = {
    val aux = state.lastSyncedBlockEpochSecondNano
    com.google.protobuf.timestamp.Timestamp(aux._1, aux._2)
  }
