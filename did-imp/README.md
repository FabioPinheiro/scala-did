# did-imp

The plan is to eventually have a fully functional implementation for JVM and JS platforms!

## TODO

- Fix all FIXME and remove the ???
- Have our implementation of:
  - AdditionalAuthenticatedData - https://www.rfc-editor.org/rfc/rfc7518#section-5
- Keep meta information when decrypting a message
- Fail if the message decrypted is authenticated but does not match the 'from' field


## JS with WASM

Another approach is to use the RUST libraries for all the cryptography.
Call those methods through WebAssembly.

User `wasm-pack` to build the wasm.
Ex project https://github.com/rustwasm/wasm-pack-template build with `wasm-pack build --target web`

Config `vite.config.js` with `import wasm from "vite-plugin-wasm";`

### ScalaJS

Install the JS package with `npm install --save ./wasm/pkg`

```scala
import org.scalajs.dom._
val importObject = js.Dynamic.literal(imports = js.Dynamic.literal(imported_func = (p1) => println(p1)))
js.Dynamic.global.WebAssembly
  .instantiateStreaming(fetch("simple.wasm"), importObject)
  .`then`((obj: js.Dynamic) => obj.instance.exports.exported_func())
```

The ScalablyTyped plugin will generate the Facade. It can be called like this

```scala
import typings.myproject.mod
mod
  .default() // "myproject_bg.wasm"
  .`then` { e =>
    mod.greet()
  }
```