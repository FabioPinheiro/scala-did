package fmgp.did.method.prism

import zio._
import fmgp.did.method.prism.cardano.TxHash
import _root_.proto.prism.SignedPrismOperation

/** Inspiration from Git */
trait PrismChainService {
  case class Commit(msg: Option[String], prismEvents: Seq[SignedPrismOperation])

  /** Note this state includes localCommits chain */
  // def prismState: ZIO[Any, Nothing, Ref[PrismState]]
  // def upstreamPrismState: ZIO[Any, Nothing, Ref[PrismState]]
  // def pull: ZIO[Any, Nothing, Ref[PrismState]]
  // def localCommits: Seq[Commit]
  // def squashLocalCommit: Commit

  // /** make a transation */
  // def commit(
  //     prismEvents: Seq[SignedPrismOperation],
  //     maybeMsgCIP20: Option[String],
  // ): ZIO[Any, Throwable, search the]

  // def push(): ZIO[BlockfrostConfig, Throwable, (Int, String)]

  def commitAndPush(
      prismEvents: Seq[SignedPrismOperation],
      msg: Option[String],
  ): ZIO[Any, Throwable, TxHash]
}
