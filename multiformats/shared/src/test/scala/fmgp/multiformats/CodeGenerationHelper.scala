package fmgp.multiformats

import scala.io.Source

//  sbt "multiformatsJVM/Test/runMain fmgp.multiformats.CodeGenerationHelper" >> Multicodec
@main def CodeGenerationHelper() = {
  Source.fromResource("table.csv").getLines().toVector.map(line => parseCsvLine(line))

  def parseCsvLine(line: String) = {
    line.split(",").toVector.map(_.trim) match {
      case Vector(name, tag, code, status) =>
        println(
          s"""  case ${name
              .replace('-', '_')} extends Codec("$name", "${tag.toUpperCase}", $code, ${status.toUpperCase})"""
        )
      case Vector(name, tag, code, status, description) =>
        println(s"""  /** $description */""")
        println(
          s"""  case ${name
              .replace('-', '_')} extends Codec("$name", "${tag.toUpperCase}", $code, ${status.toUpperCase})"""
        )
      case _ =>
        println(s"WARNING UNKNOWN DATA FORMAT FOR LINE: $line")
        None
    }
  }
}
