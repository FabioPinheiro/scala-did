package fmgp.did.comm

extension (mediaType: MediaTypes)
  def asContentType = {
    def f(mt: MediaTypes) = zio.http.Header.ContentType(zio.http.MediaType(mt.mainType, mt.subType))
    mediaType match
      case MediaTypes.PLAINTEXT => f(MediaTypes.PLAINTEXT)
      case MediaTypes.SIGNED    => f(MediaTypes.SIGNED)
      case MediaTypes.ENCRYPTED | MediaTypes.ANONCRYPT | MediaTypes.AUTHCRYPT | MediaTypes.ANONCRYPT_SIGN |
          MediaTypes.AUTHCRYPT_SIGN | MediaTypes.ANONCRYPT_AUTHCRYPT =>
        f(MediaTypes.ENCRYPTED)
      case MediaTypes.DIGITAL_CREDENTIAL_SDJWT | MediaTypes.DIGITAL_CREDENTIAL_SDJWT_OLD =>
        f(MediaTypes.DIGITAL_CREDENTIAL_SDJWT)
      case MediaTypes.BINDING_JWT => f(MediaTypes.BINDING_JWT)
  }
