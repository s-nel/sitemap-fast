package com.snacktrace.sitemapfast.impl

import com.snacktrace.sitemapfast._
import fs2._
import org.scalatest.{MustMatchers, WordSpec}
import sitemap._

class DefaultSitemapGeneratorTest extends WordSpec with MustMatchers {
  trait Fixture {
    implicit val S = fs2.Strategy.fromFixedDaemonPool(1000, threadName = "worker")
  }

  "DefaultSitemapGenerator.generate" should {
    "generate index, sitemap for single url" in new Fixture {
      generate()(Stream.eval(Task.delay(List(Url(Loc("http://example.com"), None, None, None))))).unsafeRun
    }

    "generate index, sitemap for two url" in new Fixture {
      generate()(Stream.eval(Task.delay(List(
        Url(Loc("http://example.com"), None, None, None),
        Url(Loc("http://example.com2"), None, None, None))))).unsafeRun
    }

    "gene" in new Fixture {
      val urls = List.fill(50001)(Url(Loc("http://example.com"), None, None, None))
      generate(maxOpen = 1000)(Stream.eval(Task.delay(urls))).unsafeRun
    }
  }
}
