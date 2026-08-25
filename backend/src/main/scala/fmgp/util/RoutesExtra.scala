package fmgp.util

import zio.*
import zio.http.*

extension [Env, Err](r: Routes[Env, Err])
  def logErrorAndRespond(using trace: Trace): Routes[Env, Nothing] =
    Routes.fromIterable(
      r.routes
        .map(e => e.transform(x => x.tapErrorCauseZIO(c => ZIO.logErrorCause(c))))
        .map(_.handleErrorCause(Response.fromCause(_)))
    )
