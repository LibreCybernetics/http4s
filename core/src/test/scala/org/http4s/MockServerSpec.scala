package org.http4s

import scala.language.implicitConversions
import concurrent.Future
import scala.language.reflectiveCalls

import concurrent.{Promise, Future, Await}
import scala.concurrent.duration._

import org.specs2.mutable.Specification
import play.api.libs.iteratee._
import org.specs2.time.NoTimeConversions
import scala.io.Codec

import Writable._
import java.nio.charset.Charset
import akka.util.ByteString

class MockServerSpec extends Specification with NoTimeConversions {
  import scala.concurrent.ExecutionContext.Implicits.global

  val server = new MockServer(ExampleRoute())

  def response(req: RequestPrelude, body: Enumerator[HttpChunk] = Enumerator.eof): MockServer.Response = {
    Await.result(server(req, body), 5 seconds)
  }

  "A mock server" should {
    "handle matching routes" in {
      val req = RequestPrelude(requestMethod = Method.Post, pathInfo = "/echo")
      val body = Enumerator("one", "two", "three").map[HttpChunk](s => HttpEntity(ByteString(s, req.charset.name)))
      new String(response(req, body).body) should_==("onetwothree")
    }

    "runs a sum" in {
      val req = RequestPrelude(requestMethod = Method.Post, pathInfo = "/sum")
      val body = Enumerator("1\n", "2\n3", "\n4").map[HttpChunk](s => HttpEntity(ByteString(s, req.charset.name)))
      new String(response(req, body).body) should_==("10")
    }

    "runs too large of a sum" in {
      val req = RequestPrelude(requestMethod = Method.Post, pathInfo = "/sum")
      val body = Enumerator("12345678\n901234567").map[HttpChunk](s => HttpEntity(ByteString(s, req.charset.name)))
      response(req, body).statusLine should_==(Status.RequestEntityTooLarge)
    }

    "fall through to not found" in {
      val req = RequestPrelude(pathInfo = "/bielefield")
      response(req).statusLine should_== Status.NotFound
    }

    "handle exceptions" in {
      val req = RequestPrelude(pathInfo = "/fail")
      response(req).statusLine should_== Status.InternalServerError
    }

    "Handle futures" in {
      val req = RequestPrelude(pathInfo = "/future")
      val returned = response(req)
      returned.statusLine should_== Status.Ok
      new String(returned.body) should_== "Hello from the future!"
    }

    "Do a Go" in {
      val req = RequestPrelude(pathInfo = "/challenge")
      val body = Enumerator[HttpChunk](HttpEntity(ByteString("Go and do something", req.charset.name)))
      val returned = response(req, body)
      returned.statusLine should_== Status.Ok
      new String(returned.body) should_== "Go and do something"
    }

    "Do a NoGo" in {
      val req = RequestPrelude(pathInfo = "/challenge")
      val body = Enumerator[HttpChunk](HttpEntity(ByteString("NoGo and do something", req.charset.name)))
      val returned = response(req, body)
      returned.statusLine should_== Status.BadRequest
      new String(returned.body) should_== "Booo!"
    }

    "Do an Empty Body" in {
      val req = RequestPrelude(pathInfo = "/challenge")
      val returned = response(req)
      returned.statusLine should_== Status.BadRequest
      new String(returned.body) should_== "No data!"
    }
  }
}
