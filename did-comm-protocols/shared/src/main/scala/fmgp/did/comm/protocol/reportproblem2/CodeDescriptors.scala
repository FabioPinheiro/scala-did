package fmgp.did.comm.protocol.reportproblem2

trait CodeDescriptors { def code: String }

trait CD_tust

/** Failed to achieve required trust.
  *
  * Typically this code indicates incorrect or suboptimal behavior by the sender of a previous message in a protocol.
  * For example, a protocol required a known sender but a message arrived anoncrypted instead — or the encryption is
  * well formed and usable, but is considered weak. Problems with this descriptor are similar to those reported by
  * HTTP’s 401, 403, or 407 status codes.
  */
object CD_tust_ extends CD_tust { def code: String = "trust" }

/** Cryptographic operation failed.
  *
  * A cryptographic operation cannot be performed, or it gives results that indicate tampering or incorrectness. For
  * example, a key is invalid — or the key types used by another party are not supported — or a signature doesn’t verify
  * — or a message won’t decrypt with the specified key.
  */
object CD_tust_crypto_ extends CD_tust { def code: String = "trust.crypto" }

/** Unable to transport data.
  *
  * The problem is with the mechanics of moving messages or associated data over a transport. For example, the sender
  * failed to download an external attachment — or attempted to contact an endpoint, but found nobody listening on the
  * specified port.
  */
object CD_xfer extends CodeDescriptors { def code: String = "xfer" }

/** DID is unusable. - A DID is unusable because its method is unsupported — or because its DID doc cannot be parsed —
  * or because its DID doc lacks required data.
  */
object CD_did extends CodeDescriptors { def code: String = "did" }

/** Bad message.
  *
  * Something is wrong with content as seen by application-level protocols (i.e., in a plaintext message). For example,
  * the message might lack a required field, use an unsupported version, or hold data with logical contradictions.
  * Problems in this category resemble HTTP’s 400 status code.
  */
object CD_msg extends CodeDescriptors { def code: String = "msg" }

/** Internal error.
  *
  * The problem is with conditions inside the problem sender’s system. For example, the sender is too busy to do the
  * work entailed by the next step in the active protocol. Problems in this category resemble HTTP’s 5xx status codes.
  */
object CD_me extends CodeDescriptors { def code: String = "me" }

/** A required resource is inadequate or unavailable. - The following subdescriptors are also defined: me.res.net,
  * me.res.memory, me.res.storage, me.res.compute, me.res.money
  */
object CD_me_res extends CodeDescriptors { def code: String = "me.res" }

/** Circumstances don’t satisfy requirements.
  *
  * A behavior occurred out of order or without satisfying certain preconditions — or circumstances changed in a way
  * that violates constraints. For example, a protocol that books plane tickets fails because, halfway through, it is
  * discovered that all tickets on the flight have been sold.
  */
object CD_req extends CodeDescriptors { def code: String = "req" }

/** Failed to satisfy timing constraints.
  *
  * A message has expired — or a protocol has timed out — or it is the wrong time of day/day of week.
  */
object CD_req_time extends CodeDescriptors { def code: String = "req.time" }

/** Failed for legal reasons.
  *
  * An injunction or a regulatory requirement prevents progress on the workflow. Compare HTTP status code 451.
  */
object CD_legal extends CodeDescriptors { def code: String = "legal" }
