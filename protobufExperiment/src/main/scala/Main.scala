import scalapb.protos._

object Main {

  def main(args: Array[String]) = {
    val aux = scalapb.protos.demo.Plaintext(
      id = "5c94cc16-8dc9-4e39-b55f-4f86fb8ee72e",
      `type` = "https://didcomm.org/trust-ping/2.0/ping",
      to = Seq(
        "did:peer:2.Ez6LSkGy3e2z54uP4U9HyXJXRpaF2ytsnTuVgh6SNNmCyGZQZ.Vz6Mkjdwvf9hWc6ibZndW9B97si92DSk9hWAhGYBgP9kUFk8Z.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9ib2IuZGlkLmZtZ3AuYXBwLyIsInIiOltdLCJhIjpbImRpZGNvbW0vdjIiXX0"
      ),
      from =
        "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ",
      body = Some(
        scalapb.protos.demo
          .Body(responseRequested = true)
      ),
    )

    println("protobuf: " + aux.toProtoString)
    println(aux.serializedSize)
    println(aux.toByteArray.size)

    val json =
      """{"id":"5c94cc16-8dc9-4e39-b55f-4f86fb8ee72e","type":"https://didcomm.org/trust-ping/2.0/ping","to":["did:peer:2.Ez6LSkGy3e2z54uP4U9HyXJXRpaF2ytsnTuVgh6SNNmCyGZQZ.Vz6Mkjdwvf9hWc6ibZndW9B97si92DSk9hWAhGYBgP9kUFk8Z.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9ib2IuZGlkLmZtZ3AuYXBwLyIsInIiOltdLCJhIjpbImRpZGNvbW0vdjIiXX0"],"from":"did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ","body":{"response_requested":true}}"""
    println("json: " + json)
    println(json.getBytes().size)

    println(1d * aux.serializedSize * 100 / json.getBytes().size) // => 89.24731182795699

    //
    val a1 = aux.to.head.getBytes().size + aux.from.getBytes().size
    println(1d * a1 * 100 / json.getBytes().size) // FROM and TO => 73.29749103942652

    println(1d * aux.`type`.getBytes().size * 100 / json.getBytes().size) // => 6.989247311827957

  }
}
