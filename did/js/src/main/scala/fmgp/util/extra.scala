package fmgp.util

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array
import scala.scalajs.js.typedarray.ArrayBuffer

import scala.scalajs.js.JSConverters.*

def byteArray2Uint8Array(arr: Array[Byte]): Uint8Array = Uint8Array.from(arr.toJSArray.map(_.toShort))

def uint8Array2ByteArray(arr: Uint8Array): Array[Byte] = Uint8Array.from(arr).map(_.toByte).toArray

def arrayBuffer2ByteArray(arr: ArrayBuffer): Array[Byte] = Uint8Array(arr).map(_.toByte).toArray
