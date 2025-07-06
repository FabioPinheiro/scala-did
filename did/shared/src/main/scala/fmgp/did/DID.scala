package fmgp.did

import zio.json._
import zio.json.ast.Json
import fmgp.did.comm.{FROM, FROMTO, TO}

//https://github.com/jwtk/jjwt
//https://github.com/panva/jose

//JSON-LD
//https://www.w3.org/ns/did/v1

type Required[A] = A
type NotRequired[A] = Option[A]
type SetU[A] = A | Seq[A] // TODO https://github.com/FabioPinheiro/scala-did/issues/322
// type SetMapU[A] = A | Seq[A] | Map[String, A]
// type ServiceEndpoint = URI | Map[String, URI] | Seq[URI] | Seq[Map[String, URI]] //SetU[URI]
type ServiceEndpoint = Json.Str | Json.Obj | Json.Arr
type ServiceEndpointNoStr = Json.Obj | Json.Arr
type Authentication = Option[Set[VerificationMethod]]

/** To use the decoder and encoder: import fmgp.did.SetU.given */
object SetU {
  given decoder[U](using jsonDecoder: JsonDecoder[U]): JsonDecoder[U | Seq[U]] =
    jsonDecoder
      .map(e => e: U | Seq[U])
      .orElse(JsonDecoder.seq[U].map(e => e: U | Seq[U]))

  // opinionated will always and go to a sequence
  // inline given encoder[U](using jsonEncoder: JsonEncoder[U]): JsonEncoder[U | Seq[U]] =
  //   JsonEncoder.seq[U].contramap { (uuu: (U | Seq[U])) =>
  //     uuu match {
  //       case one: U                 => Seq(one)
  //       case seq: Seq[U] @unchecked => seq
  //     }
  //   }

  // TODO we must prove that [U] is not the sequence itself... this will not work in that case
  given encoder[U](using jsonEncoder: JsonEncoder[U]): JsonEncoder[U | Seq[U]] =
    new JsonEncoder[U | Seq[U]] {
      override def unsafeEncode(b: U | Seq[U], indent: Option[Int], out: zio.json.internal.Write): Unit =
        if (b.isInstanceOf[Seq[?]])
          JsonEncoder.seq[U].unsafeEncode(b.asInstanceOf[Seq[U]], indent, out)
        else jsonEncoder.unsafeEncode(b.asInstanceOf[U], indent, out)

    }

}

object ServiceEndpoint {
  given decoder: JsonDecoder[ServiceEndpoint] =
    summon[JsonDecoder[Json]].mapOrFail {
      case j: Json.Null => Left("ServiceEndpoint can not be 'null'")
      case j: Json.Bool => Left("ServiceEndpoint can not be Boolean")
      case j: Json.Num  => Left("ServiceEndpoint can not be Numbre")
      case j: Json.Arr =>
        j match
          case e if e.elements.toVector.forall(_.isInstanceOf[Json.Str]) => Right(j)
          case e if e.elements.toVector.forall(_.isInstanceOf[Json.Obj]) => Right(j)
          case e => Left("ServiceEndpoint can be Array can olny be of Strings of Objects")
      case j: Json.Str => Right(j)
      case j: Json.Obj => Right(j)
    }

  given encoder: JsonEncoder[ServiceEndpoint] =
    summon[JsonEncoder[Json]].contramap(e => e)
}

trait DID {
  final def scheme: String = "did"
  def namespace: String // methodName
  def specificId: String

  /** This is the full identifier */
  final def string = s"$scheme:$namespace:$specificId"
  final def did = string

  // override def toString(): String = string

  def asDIDSubject = DIDSubject(scheme + ":" + namespace + ":" + specificId)
  def asTO = TO.unsafe_apply(scheme + ":" + namespace + ":" + specificId)
  def asFROM = FROM.unsafe_apply(scheme + ":" + namespace + ":" + specificId)
  def asFROMTO = FROMTO.unsafe_apply(scheme + ":" + namespace + ":" + specificId)
}
object DID {
  given Conversion[DID, DIDSubject] = _.asDIDSubject // FIXME REMOVE
  given Conversion[DID, TO] = _.asTO // FIXME REMOVE
  given Conversion[DID, FROM] = _.asFROM // FIXME REMOVE
  given Conversion[DID, FROMTO] = _.asFROMTO // FIXME REMOVE

  val regex = """^did:([^\s:]+):([^\?\#\s]+)(?!\?[^\#\s:]*)(?!\#.*)$""".r // OLD """^did:([^\s:]+):([^\s]+)$""".r
}

type DIDSyntax = String //FIXME
type DIDURLSyntax = String //FIXME
type DIDController = DIDSyntax //FIXME
/** RFC3986 - https://www.rfc-editor.org/rfc/rfc3986 */
type URI = String

/** @see https://www.w3.org/TR/xmlschema11-2/#dateTimeStamp */
type DateTimeStamp = String

trait JSONLD {
  def `@context`: String | Seq[String] // = "https://w3id.org/did/v1" // JSON-LD object
}

case class SingleJSONLD(`@context`: String) extends JSONLD
