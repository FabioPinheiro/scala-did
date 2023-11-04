package fmgp.did.comm

import zio.http.Header
import zio.http.MediaType

extension (mediaType: MediaTypes)
  def asContentType = mediaType match
    case MediaTypes.PLAINTEXT =>
      Header.ContentType(MediaType(MediaTypes.PLAINTEXT.mainType, MediaTypes.PLAINTEXT.subType))
    case MediaTypes.SIGNED =>
      Header.ContentType(MediaType(MediaTypes.SIGNED.mainType, MediaTypes.SIGNED.subType))
    case MediaTypes.ENCRYPTED | MediaTypes.ANONCRYPT | MediaTypes.AUTHCRYPT | MediaTypes.ANONCRYPT_SIGN |
        MediaTypes.AUTHCRYPT_SIGN | MediaTypes.ANONCRYPT_AUTHCRYPT =>
      Header.ContentType(MediaType(MediaTypes.ENCRYPTED.mainType, MediaTypes.ENCRYPTED.subType))
