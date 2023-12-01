# Import Library

The frist step is to import the Library that you need.

## Getting Started with Scala-did

To install ScalaDID in a scala project

```scala
 libraryDependencies += "app.fmgp" %% "did" % @VERSION@ // for DID and DID Comm
 libraryDependencies += "app.fmgp" %% "did-imp" % @VERSION@ // for crypto implementation
 libraryDependencies += "app.fmgp" %% "did-framework" % @VERSION@ //for utils
 libraryDependencies += "app.fmgp" %% "did-peer" % @VERSION@ // for resolver of the did method `peer`
 libraryDependencies += "app.fmgp" %% "did-web" % @VERSION@ // for resolver the did method `web`
 libraryDependencies += "app.fmgp" %% "did-uniresolver" % @VERSION@ // for calling the resolver uniresolver
```

In a crossProject for the JSPlatform and JVMPlatform this should be `%%%` instead of `%%`

You can check the latest available of versions (for JVM and JS) in here:

![Maven Central](https://img.shields.io/maven-central/v/app.fmgp/did_3)
[![did Scala version support](https://index.scala-lang.org/fabiopinheiro/scala-did/did/latest-by-scala-version.svg?platform=jvm)](https://index.scala-lang.org/fabiopinheiro/scala-did/did)
[![did Scala version support](https://index.scala-lang.org/fabiopinheiro/scala-did/did/latest-by-scala-version.svg?platform=sjs1)](https://index.scala-lang.org/fabiopinheiro/scala-did/did)


## Modules

Let got module by module 
- `did` - 
- `did-imp`
- `did-framework`

- `did-peer` & `did-web` is our implementation of of the `did:peer` and `did:web` methods.
- `did-uniresolver` is an intregation with the [uniresolver](https://dev.uniresolver.io/).
  So you can use the uniresolver to resolver the `did methods`.
  You can use the publicly available uniresolver or a custom one.


- `did-example` is just a list DID knowd identities, Like mediator and other agents. Also, contends some utilities.
- `multiformats` is our implementation of [Multiformats](https://multiformats.io/) used by `did-peer`