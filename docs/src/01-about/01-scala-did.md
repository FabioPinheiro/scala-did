# Scala DID

Scala DID is a Scala/ScalaJS library for DID and DIDcomm v2.

Decentralized Identifiers (DID) are a new type of identifier that enables verifiable, decentralized digital identity.

DIDComm Messaging (or just DIDComm) is a secure messaging protocol for DIDs.

This library allows you to easily integrate DID and DIDComm messaging into your application.

The one of the main goals of this library is to make DID and DIDComm v2 **type safety** and **easy to use**.

The [repository](https://github.com/FabioPinheiro/scala-did) also contes the [**LIVE DEMO (DIDComm's Sandbox)**](https://did.fmgp.app/)
The **PoC Mediator** was move to the [Identus Mediator](https://github.com/hyperledger/identus-mediator) where a production ready mediator is being developed and maintain.

## Badges
![Maven Central](https://img.shields.io/maven-central/v/app.fmgp/did_3)
[![CI](https://github.com/FabioPinheiro/scala-did/actions/workflows/ci.yml/badge.svg)](https://github.com/FabioPinheiro/scala-did/actions/workflows/ci.yml)
[![Scala Steward](https://github.com/FabioPinheiro/scala-did/actions/workflows/scala-steward.yml/badge.svg)](https://github.com/FabioPinheiro/scala-did/actions/workflows/scala-steward.yml)

[![did Scala version support](https://index.scala-lang.org/fabiopinheiro/scala-did/did/latest-by-scala-version.svg?platform=jvm)](https://index.scala-lang.org/fabiopinheiro/scala-did/did)
[![did Scala version support](https://index.scala-lang.org/fabiopinheiro/scala-did/did/latest-by-scala-version.svg?platform=sjs1)](https://index.scala-lang.org/fabiopinheiro/scala-did/did)

## Design Goals

### Specification compliant

We tryied to follow the [DID Comm Specification](https://identity.foundation/didcomm-messaging/spec/).

### Purely Functional

The Library API is **purely functional**.
The internal implementetion tries also to be **purely functional**, and any call to the dependency libory (for cryptography) encapsulates and handel the error in a Functional way.
All the error are value so the user know exactly what to expect by the signatures of the methods. 

### Simples to use

`"But I just want to send/read a message!"` - We are create a Framework to facilitate some of the mundane tasks and boilerplate code. Like to open ws connections or making http calls.

We also have an implementation for most of the [DID Comm Protocalls](https://didcomm.org/) of the official repository.

## WebSite Map
