package fmgp.did.method.prism.node

import fmgp.did.method.prism._

extension (state: PrismState)
  def lastSyncedBlockTimestamp: com.google.protobuf.timestamp.Timestamp = {
    val aux = state.lastSyncedBlockEpochSecondNano
    com.google.protobuf.timestamp.Timestamp(aux._1, aux._2)
  }
