package com.snacktrace.sitemapfast.impl

import java.nio.file.{Path, Paths}
import java.text.SimpleDateFormat
import java.util.Date

import com.snacktrace.sitemapfast._
import fs2._
import fs2.io.file._
import fs2.concurrent._
import fs2.compress._
import org.slf4j.{Logger, LoggerFactory}

import scala.xml.PrettyPrinter

object sitemap {
  final case class Url(loc: Loc, lastMod: Option[LastMod], changeFreq: Option[ChangeFreq], priority: Option[Priority])

  final case class Loc(value: String) extends AnyVal

  final case class LastMod(value: Date) extends AnyVal

  sealed trait ChangeFreq
  object Always extends ChangeFreq
  object Hourly extends ChangeFreq
  object Daily extends ChangeFreq
  object Weekly extends ChangeFreq
  object Monthly extends ChangeFreq
  object Yearly extends ChangeFreq
  object Never extends ChangeFreq

  final case class Priority(value: Float) extends AnyVal

  type Writer = Stream[Task, Stream[Task, List[Url]]] => Task[Unit]

  def generate
    (urlsPerSitemap: Int = 50000, sitemapsPerSitemapIndex: Int = 50000, bufferSize: Int = 4096,
      logger: Logger = LoggerFactory.getLogger(getClass), maxOpen: Int = 10,
      tmpFilePath: Path = Paths.get("/tmp/"), dateFormat: String = "yyyy-MM-dd'T'HH:mm:ssX",
      prettyPrinter: PrettyPrinter = new PrettyPrinter(120, 2))(urls: Stream[Task, List[Url]])
    (implicit strategy: Strategy): Task[Unit] = {
    urls
      .flatMap { list =>
        Stream.emits(list)
      }
      .chunkN(urlsPerSitemap)
      .zipWithIndex
      .map { tup =>
        (tup._1.flatMap(chunk => chunk.toList), tup._2)
      }
      .flatMap { tup =>
        val sitemapFileName = s"sitemap-${tup._2 + 1}.xml.gz"
        (
          Stream.emit(
            """
              |<?xml version="1.0" encoding="UTF-8"?>
              |<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
            """.stripMargin) ++
          Stream
            .emits(tup._1)
            .buffer(bufferSize)
            .map(urlToXmlString(prettyPrinter, dateFormat)) ++
          Stream.emit("</urlset>")
        )
          .through(text.utf8Encode[Task])
          .through(deflate())
          .through(writeAllAsync[Task](tmpFilePath.resolve(sitemapFileName)))
          .fold(sitemapFileName)((a, b) => a)
      }
      .chunkN(sitemapsPerSitemapIndex)
      .zipWithIndex
      .map { tup =>
        (tup._1.flatMap(chunk => chunk.toList), tup._2)
      }
      .flatMap { tup =>
        val sitemapIndexFileName = s"sitemap-index-${tup._2 + 1}.xml.gz"
        (
          Stream.emit(
            """
              |<?xml version="1.0" encoding="UTF-8"?>
              |<sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
            """.stripMargin) ++
          Stream
            .emits(tup._1)
            .buffer(4096)
            .map(sitemapUrlToXmlString(prettyPrinter, dateFormat)) ++
          Stream.emit("</sitemapindex>")
        )
          .through(text.utf8Encode[Task])
          .through(deflate())
          .through(writeAllAsync[Task](tmpFilePath.resolve(sitemapIndexFileName)))
          .fold(sitemapIndexFileName)((a, b) => a)
      }
      .run
  }

  private def changeFreqToString(changeFreq: ChangeFreq): String = changeFreq match {
    case Always => "always"
    case Hourly => "hourly"
    case Daily => "daily"
    case Weekly => "weekly"
    case Monthly => "monthly"
    case Yearly => "yearly"
    case Never => "never"
  }

  private def urlToXmlString(prettyPrinter: PrettyPrinter, dateFormat: String)(url: Url): String = {
    val formattedChangeFreqOpt = url.changeFreq.map(changeFreqToString)
    val formattedPriorityOpt = url.priority.map(priority => priority.value.toString)
    val lastModOpt = url.lastMod.map(lastMod => new SimpleDateFormat(dateFormat).format(lastMod.value))
    prettyPrinter.format(<url>
      <loc>{url.loc.value}</loc>
      { if (lastModOpt.isDefined) { <lastmod>{ lastModOpt.get }</lastmod> } }
      { if (formattedChangeFreqOpt.isDefined) { <changefreq>{ formattedChangeFreqOpt.get }</changefreq> } }
      { if (formattedPriorityOpt.isDefined) { <priority>{ formattedPriorityOpt.get }</priority> } }
    </url>)
  }

  private def sitemapUrlToXmlString(prettyPrinter: PrettyPrinter, dateFormat: String)(sitemapUrl: String): String = {
    prettyPrinter.format(<sitemap>
      <loc>{sitemapUrl}</loc>
      <lastmod>{new SimpleDateFormat(dateFormat).format(new Date())}</lastmod>
    </sitemap>)
  }
}
