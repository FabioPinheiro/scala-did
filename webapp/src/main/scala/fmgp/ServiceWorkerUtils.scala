package fmgp

import zio._
import zio.json._

import org.scalajs.dom._
import org.scalajs.dom.experimental.push._
import org.scalajs.dom.experimental.serviceworkers._
import org.scalajs.dom.experimental.{Notification, NotificationOptions}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Random, Success}

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.typedarray.Uint8Array
import fmgp.did.Client
import fmgp.Config.PushNotifications

case class NotificationsSubscription(
    endpoint: String,
    // expirationTime: Option[String],
    keys: NotificationsSubscription.Key,
    id: Option[String]
) {
  def name: String = id.getOrElse("?_?")
}

object NotificationsSubscription {
  given decoder: JsonDecoder[NotificationsSubscription] = DeriveJsonDecoder.gen[NotificationsSubscription]
  given encoder: JsonEncoder[NotificationsSubscription] = DeriveJsonEncoder.gen[NotificationsSubscription]

  case class Key(p256dh: String, auth: String)

  object Key {
    given decoder: JsonDecoder[Key] = DeriveJsonDecoder.gen[Key]
    given encoder: JsonEncoder[Key] = DeriveJsonEncoder.gen[Key]
  }

  // def fromPushSubscription(pushSubscription: PushSubscription): Either[String, NotificationsSubscription] =
  //   js.JSON.stringify(pushSubscription.toJSON()).fromJson[NotificationsSubscription]
  def unsafeFromPushSubscription(pushSubscription: PushSubscription, id: String = ""): NotificationsSubscription =
    NotificationsSubscription(
      endpoint = pushSubscription.endpoint,
      // expirationTime = pushSubscription.toJSON().expirationTime,
      keys = Key(
        p256dh = pushSubscription.toJSON().keys.get("p256dh").get,
        auth = pushSubscription.toJSON().keys.get("auth").get
      ),
      id = if (id.trim.isEmpty) None else Some(id.trim)
    )

}

def base642Uint8Array(base64: String): Uint8Array =
  byteArray2Uint8Array(java.util.Base64.getUrlDecoder.decode(base64))

def byteArray2Uint8Array(arr: Array[Byte]): Uint8Array = Uint8Array(arr.toJSArray.map(_.toShort))
// def byteArray2Uint8Array(arr: Array[Byte]): Uint8Array =
// js.Dynamic.newInstance(js.Dynamic.global.Uint8Array)(arr.toJSArray).asInstanceOf[Uint8Array]

@scala.scalajs.js.annotation.JSExportTopLevel("ServiceWorkerUtils")
object ServiceWorkerUtils {

  @scala.scalajs.js.annotation.JSExport
  def runRegisterServiceWorker =
    Unsafe.unsafe { implicit unsafe => // Run side effect
      Runtime.default.unsafe.runToFuture(registerServiceWorker)
    }
  @scala.scalajs.js.annotation.JSExport
  def runSubscribeToNotifications(pushNotificationsPublicKey: String) =
    Unsafe.unsafe { implicit unsafe => // Run side effect
      Runtime.default.unsafe.runToFuture(subscribeToNotifications(pushNotificationsPublicKey))
    }
  @scala.scalajs.js.annotation.JSExport
  def runPushNotificationsSubscription(id: String) =
    Unsafe.unsafe { implicit unsafe => // Run side effect
      Runtime.default.unsafe.runToFuture(pushNotificationsSubscription(id))
    }

  /** //navigator.serviceWorker.register() is effectively a no-op during subsequent visits. When it's called is
    * irrelevant.
    *
    * @see
    *   https://developers.google.com/web/fundamentals/primers/service-workers/lifecycle
    */
  @scala.scalajs.js.annotation.JSExport
  def registerServiceWorker: ZIO[Any, Throwable, Unit] = {
    ZIO.log(s"### call to registerServiceWorker (${fmgp.SettingsFromHTML.getModeInfo})") *>
      ZIO
        .fromPromiseJS(fmgp.SettingsFromHTML.serviceWorkerContainer)
        .flatMap { registration =>
          ZIO.log("registerServiceWorker: registered service worker") *>
            ZIO.fromPromiseJS(registration.update()) *>
            // subscribeToNotifications(PushNotifications.applicationServerKey) *>
            ZIO.unit
        }
        .tapError(ex =>
          ZIO.logError(s"registerServiceWorker: service worker registration failed > $ex: ${ex.printStackTrace()}")
        )
  }

  @scala.scalajs.js.annotation.JSExport
  def subscribeToNotifications(pushNotificationsPublicKey: String): ZIO[Any, Throwable, PushSubscription] = {
    ZIO.log("### call to subscribeToNotifications!") *>
      ZIO.fromPromiseJS(window.navigator.serviceWorker.ready).flatMap { registration =>
        val pushManager: PushManager = registration.pushManager
        val pushSubscriptionOptions = new PushSubscriptionOptions {
          userVisibleOnly = true
          applicationServerKey = base642Uint8Array(pushNotificationsPublicKey)
        }
        ZIO
          .fromPromiseJS(pushManager.subscribe(pushSubscriptionOptions))
          .flatMap { case pushSubscription: PushSubscription =>
            ZIO.log(s"Subscribe To Notifications return: ${js.JSON.stringify(pushSubscription.toJSON())}") *>
              ZIO.succeed(pushSubscription)
          }
          .tapError(ex => ZIO.logError(s"Fail to subscribeToNotifications with exception: $ex"))
      }
  }

  def pushNotificationsSubscription(id: String) = {
    ZIO.fromPromiseJS(window.navigator.serviceWorker.ready).flatMap { registration =>
      val pushManager: PushManager = registration.pushManager
      ZIO
        .fromPromiseJS(pushManager.getSubscription())
        .tapError(ex => ZIO.log(s"Fail to getNotificationsSubscription with exception: $ex"))
        .flatMap(pushSubscription =>
          val ns = NotificationsSubscription.unsafeFromPushSubscription(pushSubscription)
          ZIO.log(s"My NotificationsSubscription is: ${js.JSON.stringify(pushSubscription.toJSON())}") *>
            Client
              .pushNotificationsSubscription(ns)
              .catchAll { case str: String => ZIO.logError(str) }
        )
    }
  }

  @scala.scalajs.js.annotation.JSExport
  def requestNotificationPermission = {
    def aux(status: String) = println(s"Notification permission status: $status")
    dom.Notification.requestPermission(aux _)
  }

}
