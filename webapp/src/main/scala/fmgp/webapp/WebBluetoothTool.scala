package fmgp.webapp

import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import typings.std.stdStrings.text
import typings.mermaid

import fmgp.did._
object WebBluetoothTool {

  def apply(): HtmlElement = // rootElement
    div(
      p("WebBluetoothTool"),
      p("The Discord bot is this a WIP (2023-09-10)"),
      p(
        "Specs of ",
        a(
          href := "https://translate.google.pt/?sl=en&tl=pt&text=we%20did%20come%20capabilities&op=translate",
          "Web Bluetooth API"
        ),

        // chrome://flags/
        // Web Bluetooth
        // Enables the Web Bluetooth API on platforms without official support – Linux
        // #enable-web-bluetooth
      ),

      //
    )

}
// chrome://flags/
// https://developer.mozilla.org/en-US/docs/Web/API/Web_Share_API
// https://developer.mozilla.org/en-US/docs/Web/Manifest/share_target
// https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/How_to/Share_data_between_apps
// https://w3c.github.io/web-share/demos/share-files.html

// 413 Request Entity Too Large

// curl 'http://localhost:8080/' \
//   -X 'POST' \
//   -H 'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7' \
//   -H 'Accept-Language: en,en-US;q=0.9,pt;q=0.8,pt-PT;q=0.7' \
//   -H 'Cache-Control: max-age=0' \
//   -H 'Connection: keep-alive' \
//   -H 'Content-Length: 4149152' \
//   -H 'Content-Type: multipart/form-data; boundary=----MultipartBoundary--sj7C6H3FltmnNsg5YSpZwrRGetZl8mfCRKywygDQws----' \
//   -H 'DNT: 1' \
//   -H 'Origin: null' \
//   -H 'Sec-Fetch-Dest: document' \
//   -H 'Sec-Fetch-Mode: navigate' \
//   -H 'Sec-Fetch-Site: none' \
//   -H 'Sec-Fetch-User: ?1' \
//   -H 'Upgrade-Insecure-Requests: 1' \
//   -H 'User-Agent: Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36' \
//   -H 'sec-ch-ua: "Google Chrome";v="119", "Chromium";v="119", "Not?A_Brand";v="24"' \
//   -H 'sec-ch-ua-mobile: ?1' \
//   -H 'sec-ch-ua-platform: "Android"' \
//   --compressed
