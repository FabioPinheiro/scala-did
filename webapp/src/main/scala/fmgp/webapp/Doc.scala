/*
package fmgp.webapp

import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import typings.std.stdStrings.text
import typings.mermaid

import fmgp.did._

import laika.api._
import laika.ast._
import laika.format._
import laika.format.Markdown.GitHubFlavor
import laika.config.SyntaxHighlighting

import org.scalajs.dom.{DOMParser, MIMEType}
import fmgp.webapp.MyRouter.DocPage
import cats.data.NonEmptyChainImpl

object Doc {

  val transformer = Transformer
    .from(Markdown)
    .to(HTML)
    .using(GitHubFlavor, SyntaxHighlighting)
    .usingSpanRule {
      //   // case debug =>
      //   //   println(debug)
      //   //   RewriteAction.Retain
      //   case debug: CodeBlock =>
      //     println(debug)
      //     RewriteAction.Retain
      case LinkPathReference(content, path, source, title, options) =>
        path match
          case p @ RelativePath.Segments(segments, suffix, fragment, parentLevels) =>
            val newSegments = "." +: "#" +: "doc" +: segments.tail
            val newPath = p.copy(segments = NonEmptyChainImpl.fromChainUnsafe(newSegments))
            RewriteAction.Replace(LinkPathReference(content, newPath, source, title, options))
          case _ => RewriteAction.Retain
    }
    .build

  val DEBUG = Transformer
    .from(Markdown)
    .to(HTML)
    .using(GitHubFlavor)
    .using(SyntaxHighlighting)
    .usingSpanRule { case debug =>
      // println(debug)
      RewriteAction.Retain
    }
    .build
    .parser
    .parse(fmgp.did.DocSource._01_about_01_scala_did_md)
  // .transform(fmgp.did.DocSource.quickstart_basic_examples_md, laika.ast.Path(List("doc", "fixme")) match
  // case Left(value)  => value.message
  // case Right(value) => value
  println(DEBUG.toOption.get)

  // val htmlRenderer = Renderer.of(HTML).build

  val results = fmgp.did.DocSource.all.view
    .mapValues(data =>
      transformer.transform(data) match
        case Left(value)  => value.message
        case Right(value) => value
    )
    .toMap

  def divContainer(path: String) = {
    val tmpDiv = div()
    // val html = DOMParser().parseFromString(result, MIMEType.`text/html`)
    results.get(path) match
      case None        => tmpDiv.ref.innerText = s"Missing page for '$path'" // side effect
      case Some(value) => tmpDiv.ref.innerHTML = value // side effect
    tmpDiv
  }

  def apply(docPagePageSignal: Signal[DocPage]): HtmlElement = {
    div(
      p("DID Comm Documentation"),
      p("Page: ", child <-- docPagePageSignal.map(e => e.path.mkString(" > "))),
      child <-- docPagePageSignal.map(e => divContainer(e.path.mkString("/"))),
    )
  }
}
 */
