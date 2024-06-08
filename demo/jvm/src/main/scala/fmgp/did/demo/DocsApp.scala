package fmgp.did.demo

import zio._
import zio.http._

object DocsApp {

  // def mdocMarkdown = Routes(
  //   Method.GET / "mdoc" / string("path") -> handler { (path: String, req: Request) => path }
  //     .flatMap(path => Handler.fromResource(s"$path")),
  // ).sandbox

  def mdocHTML = Routes(
    // Method.GET / "doc" / string("path") -> handler { (path: String, req: Request) => path }
    //   .flatMap { path =>
    //     val transformer = Transformer
    //       .from(Markdown)
    //       .to(HTML)
    //       .using(GitHubFlavor, SyntaxHighlighting)
    //       .build

    //     Handler.fromResource(s"$path").mapZIO {
    //       _.body.asString.map { data =>
    //         val result = transformer.transform(data) match
    //           case Left(value)  => value.message
    //           case Right(value) => value
    //         Response.html(result)
    //       }
    //     }
    //   }

    // Method.GET / "doc" -> handler { (req: Request) =>
    //   import zio.http.template._
    //   Response
    //     .html(
    //       html(
    //         body(
    //           ul( // Custom UI to list all the files in the directory
    //             (li(a(href := "..", "..")) +: Source
    //               .fromResource("did-doc")
    //               .getLines()
    //               .map { file => li(a(href := "/doc/" + file, file)): Html }
    //               .toSeq): _*
    //           )
    //         )
    //       )
    //     )
    // },
    Method.GET / "doc" / trailing -> handler {
      val extractPath = Handler.param[(Path, Request)](_._1)
      val extractRequest = Handler.param[(Path, Request)](_._2)
      import zio.http.template._

      for {
        path <- extractPath
        file <- Handler.getResourceAsFile("did-doc/" + path.dropLeadingSlash.encode)
        http <- extractRequest >>> {
          if (file.isDirectory) {
            // Accessing the files in the directory
            val files = file.listFiles.toList.sortBy(_.getName)
            val auxPath = path./:("/doc")
            Handler.template(s"File Explorer ~$auxPath") {
              ul( // Custom UI to list all the files in the directory
                li(a(href := s"${auxPath.dropRight(1)}", "..")),
                files.map { file => li(a(href := s"${auxPath.addTrailingSlash}${file.getName}", file.getName)) },
              )
            }
          } else if (file.isFile) Handler.fromFile(file) // Return the file if it's a static resource
          else Handler.notFound // Return a 404 if the file doesn't exist
        }
      } yield http

      // (path: String, req: Request) =>
      //   // RoutesMiddleware
      //   // TODO https://zio.dev/reference/stream/zpipeline/#:~:text=ZPipeline.gzip%20%E2%80%94%20The%20gzip%20pipeline%20compresses%20a%20stream%20of%20bytes%20as%20using%20gzip%20method%3A
      //   val fullPath = s"did-doc/$path"
      //   val classLoader = Thread.currentThread().getContextClassLoader()
      //   val headerContentType = fullPath match
      //     case s if s.endsWith(".html") => Header.ContentType(MediaType.text.html)
      //     case s if s.endsWith(".js")   => Header.ContentType(MediaType.text.javascript)
      //     case s if s.endsWith(".css")  => Header.ContentType(MediaType.text.css)
      //     case s                        => Header.ContentType(MediaType.text.plain)
      //   Handler.fromResource(fullPath).map(_.addHeader(headerContentType))
    },
    Method.GET / "apis" / trailing -> handler {
      val extractPath = Handler.param[(Path, Request)](_._1)
      val extractRequest = Handler.param[(Path, Request)](_._2)
      import zio.http.template._

      for {
        path <- extractPath
        file <- Handler.getResourceAsFile("apis/" + path.dropLeadingSlash.encode)
        http <- extractRequest >>> {
          if (file.isDirectory) {
            // Accessing the files in the directory
            val files = file.listFiles.toList.sortBy(_.getName)
            val auxPath = path./:("/apis")
            Handler.template(s"File Explorer ~$auxPath") {
              ul( // Custom UI to list all the files in the directory
                li(a(href := s"${auxPath.dropRight(1)}", "..")),
                files.map { file => li(a(href := s"${auxPath.addTrailingSlash}${file.getName}", file.getName)) },
              )
            }
          } else if (file.isFile) Handler.fromFile(file) // Return the file if it's a static resource
          else Handler.notFound // Return a 404 if the file doesn't exist
        }
      } yield http
    },
    Method.GET / "api" / trailing -> handler {
      val extractPath = Handler.param[(Path, Request)](_._1)
      val extractRequest = Handler.param[(Path, Request)](_._2)
      import zio.http.template._

      for {
        path <- extractPath
        file <- Handler.getResourceAsFile("unidoc/" + path.dropLeadingSlash.encode)
        http <- extractRequest >>> {
          if (file.isDirectory) {
            // Accessing the files in the directory
            val files = file.listFiles.toList.sortBy(_.getName)
            val auxPath = path./:("/api")
            Handler.template(s"File Explorer ~$auxPath") {
              ul( // Custom UI to list all the files in the directory
                li(a(href := s"${auxPath.dropRight(1)}", "..")),
                files.map { file => li(a(href := s"${auxPath.addTrailingSlash}${file.getName}", file.getName)) },
              )
            }
          } else if (file.isFile) Handler.fromFile(file) // Return the file if it's a static resource
          else Handler.notFound // Return a 404 if the file doesn't exist
        }
      } yield http
    },
  ).sandbox

}
