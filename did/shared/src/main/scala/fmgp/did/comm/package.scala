package fmgp.did

import zio.json.ast.Json
import java.time.Instant

package object comm {

  type UTCEpoch = Long
  object UTCEpoch {
    def now = Instant.now().getEpochSecond()
  }

  type JSON_RFC7159 = Json.Obj
  object JSON_RFC7159 {
    def apply(fields: (String, Json)*): Json.Obj = Json.Obj(fields*)
  }

  type JWTToken = Json // TODO https://didcomm.org/book/v2/didrotation
}
