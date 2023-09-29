import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import { VitePWA } from 'vite-plugin-pwa'
import viteCompression from 'vite-plugin-compression';

export default defineConfig(({ command, mode, ssrBuild }) => {
  const serviceworkerSrc = '../serviceworker/target/scala-3.3.1/fmgp-serviceworker-' + ((command === 'serve') ? 'fastopt' : 'opt')
  //resolve(__dirname, 'serviceworker/target/scala-3.3.1/fmgp-serviceworker-fastopt'),

  return {
    root: './vite',
    build: {
      outDir: './dist',
      // minify: 'terser', // defualt is 'esbuild'
      // manifest: true,
      // sourcemap: true,
    },
    preview: {
      port: 8090,
      // open: true,
    },
    server: {
      cors: true,
      proxy: {
        '/ops': 'http://localhost:8080',
        '^/tap/.*': {
          target: 'ws://localhost:8080',
          ws: true,
        },
      },
    },
    plugins: [
      scalaJSPlugin({
        cwd: '.', // path to the directory containing the sbt build // default: '.'
        projectID: 'webapp', // sbt project ID from within the sbt build to get fast/fullLinkJS from
        uriPrefix: 'scalajs', // URI prefix of imports that this plugin catches
      }),
      VitePWA({
        devOptions: {
          enabled: true
        },
        srcDir: serviceworkerSrc,
        filename: "sw.js",
        strategies: "injectManifest",
        injectRegister: null, //'inline', // https://vite-pwa-org.netlify.app/guide/register-service-worker.html
        injectManifest: {
          // injectionPoint: null,
          // additionalManifestEntries: ['robots.txt'],
          maximumFileSizeToCacheInBytes: 12000000,
        },
        // includeAssets: ['favicon.ico'],
        manifest: {
          "name": "Sandbox DID Comm v2",
          "short_name": "ScalaDID",
          "description": "Demo Sandbox DID Comm v2",
          "theme_color": "#6200ee",
          "background_color": "#018786",
          "display": "standalone",
          "scope": "/",
          "start_url": "/",
          "lang": "en",
          "categories": ["education", "did"],
          "icons": [
            { "src": "https://web.dev/images/android-chrome-192x192.png", "sizes": "192x192", "type": "image/png" },
            { "src": "https://web.dev/images/android-chrome-maskable-192x192.png", "sizes": "192x192", "type": "image/png", "purpose": "maskable" },
            { "src": "https://web.dev/images/android-chrome-512x512.png", "sizes": "512x512", "type": "image/png" }
          ],
          "protocol_handlers": [{ "protocol": "did", "url": "/#/resolver/%s" }],
          "screenshots": [
            { "src": "https://web.dev/images/screenshot1.png", "sizes": "540x720", "type": "image/png" },
            { "src": "https://web.dev/images/screenshot2.png", "sizes": "540x720", "type": "image/png" },
            { "src": "https://web.dev/images/screenshot3.png", "sizes": "540x720", "type": "image/png" }
          ],
          "shortcuts": [{ "name": "Mediator", "url": "/#/mediator" }],
          "splash_pages": null
        }
      }),
      viteCompression(),
    ]
  }
});
