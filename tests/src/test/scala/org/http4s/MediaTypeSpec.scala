package org.http4s

import cats.kernel.laws.discipline.EqTests
import org.http4s.testing.HttpCodecTests
// import cats.implicits._
// import org.http4s.testing._

class MediaTypeSpec extends Http4sSpec {

  "MediaType" should {

    "Render itself" in {
      MediaType.`text/html`.renderString must_== "text/html"
    }

    "Quote extension strings" in {
      MediaType.`text/html`
        .withExtensions(Map("foo" -> "bar"))
        .renderString must_== """text/html; foo="bar""""
    }

    "Encode extensions with special characters" in {
      MediaType.`text/html`
        .withExtensions(Map("foo" -> ";"))
        .renderString must_== """text/html; foo=";""""
    }

    "Escape special chars in media range extensions" in {
      MediaType.`text/html`
        .withExtensions(Map("foo" -> "\\"))
        .renderString must_== """text/html; foo="\\""""
      MediaType.`text/html`
        .withExtensions(Map("foo" -> "\""))
        .renderString must_== """text/html; foo="\"""""
    }

    "Do a round trip through the Accept header" in {
      val raw = Header(
        "Accept",
        """text/csv;charset=UTF-8;columnDelimiter=" "; rowDelimiter=";"; quoteChar='; escapeChar="\\"""")
      raw.parsed must beAnInstanceOf[headers.Accept]
      Header("Accept", raw.parsed.value).parsed must_== raw.parsed
    }
  }

  checkAll("Eq[MediaType]", EqTests[MediaType].eqv)
  checkAll("HttpCodec[MediaType]", HttpCodecTests[MediaType].httpCodec)
}
