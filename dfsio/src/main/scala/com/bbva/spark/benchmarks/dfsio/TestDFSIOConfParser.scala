/*
 * Copyright 2017 Banco Bilbao Vizcaya Argentaria S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bbva.spark.benchmarks.dfsio

import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import scopt.OptionParser

sealed trait TestMode { def command: String }
case object Write extends TestMode { def command: String = "write" }
case object Read extends TestMode { def command: String = "read" }
case object Clean extends TestMode { def command: String = "clean" }
case object NotDefined extends TestMode { def command: String = "not-defined" }

case class TestDFSIOConf(mode: TestMode = NotDefined,
                         numFiles: Int = 4,
                         fileSize: Long = 1048576,
                         benchmarkDir: String = "/benchmarks/DFSIO",
                         resFileName: String = "TestDFSIO_results.log",
                         bufferSize: Int = 1048576,
                         hadoopExtraProps: Map[String, String] = Map.empty[String, String])

object TestDFSIOConfParser {
  @transient private lazy val logger = Logger(LoggerFactory.getLogger("IOTestBase"))

  private lazy val parser = new OptionParser[TestDFSIOConf]("TestDFSIO") {

    head(s"Test DFS I/O ${BuildInfo.version}")

    cmd("write").text(
      """Runs a test writing to the cluster. The written files are located in the DFS under the folder
        |defined by the option <outputDir>. If the folder already exists, it will be first deleted.
      """.stripMargin)
      .action((_, c) => c.copy(mode = Write))
      .children(

        opt[Int]("numFiles").required().valueName("<value>")
          .action((n, c) => c.copy(numFiles = n))
          .text("Number of files to write. Default to 4."),

        opt[String]("fileSize").required().valueName("<value>")
          .action((s, c) => c.copy(fileSize = sizeToBytes(s)))
          .validate(validateSize)
          .text("Size of each file to write (B|KB|MB|GB). Default to 1MB."),

        opt[String]("outputDir").required().valueName("<file>")
          .action((o, c) => c.copy(benchmarkDir = o))
          .text("Name of the directory to place the resultant files. Default to /benchmarks/DFSIO"),

        opt[String]("resFile").optional().valueName("<fileName>")
          .action((r, c) => c.copy(resFileName = r))
          .text("Name of the local file in the current local directory where to append the benchmark results."),

        opt[String]("bufferSize").optional().valueName("<value>")
          .action((s, c) => c.copy(fileSize = sizeToBytes(s)))
          .validate(validateSize)
          .text("Size of each file to write (B|KB|MB|GB). Default to 1MB."),

        opt[Map[String, String]]("hadoopProps").optional().valueName("k1=v1,k2=v2...")
          .action( (p, c) => c.copy(hadoopExtraProps = p))
          .text("Extra hadoop configuration properties")
      )

    cmd("read").text(
      """Runs a test reading from the cluster. It is convenient to run test with command write first, so that some
        |files are prepared for read test. If the test is run with this command before it is run with command write,
        |an error message will be shown up.
      """.stripMargin)
      .action((_, c) => c.copy(mode = Read))
      .children(

        opt[Int]("numFiles").required().valueName("<value>")
          .action((n, c) => c.copy(numFiles = n))
          .text("Number of files to read. Default to 4."),

        opt[String]("fileSize").required().valueName("<value>")
          .validate(validateSize)
          .action((s, c) => c.copy(fileSize = sizeToBytes(s)))
          .text("Size of each file to read (B|KB|MB|GB). Default to 128B."),

        opt[String]("inputDir").required().valueName("<file>")
          .action((o, c) => c.copy(benchmarkDir = o))
          .text("Name of the directory where to find the files to read. Default to /benchmarks/DFSIO"),

        opt[String]("resFile").optional().valueName("<fileName>")
          .action((r, c) => c.copy(resFileName = r))
          .text("Name of the local file in the current local directory where to append the benchmark results."),

        opt[String]("bufferSize").optional().valueName("<value>")
          .action((s, c) => c.copy(fileSize = sizeToBytes(s)))
          .validate(validateSize)
          .text("Size of each file to write (B|KB|MB|GB). Default to 1MB."),

        opt[Map[String, String]]("hadoopProps").optional().valueName("k1=v1,k2=v2...")
          .action( (p, c) => c.copy(hadoopExtraProps = p))
          .text("Extra hadoop configuration properties")
      )

    cmd("clean").text("Remove previous test data. This command deletes de output directory.")
      .action((_, c) => c.copy(mode = Clean))
      .children(
        opt[String]("outputDir").required().valueName("<file>")
          .action((o, c) => c.copy(benchmarkDir = o))
          .text("Name of the directory to clean. Default to /benchmarks/DFSIO")
      )

    checkConfig { conf =>
      if (conf.mode != NotDefined) success else failure("A command is required.")
    }

    help("help").text("prints this usage text")

    version("version")

    private val SizePattern = "^(\\d+(?:\\.\\d+)?)(([kKmMgG]?[bB]))$".r

    private def validateSize(size: String): Either[String, Unit] = {
      if (size.matches(SizePattern.toString)) success
      else failure("The size must be valid")
    }

    private def sizeToBytes(size: String): Long = {
      val units = List("b", "kb", "mb", "gb")
      val matcher = SizePattern.findFirstMatchIn(size).get
      val value = matcher.group(1)
      val unit = matcher.group(2).toLowerCase
      (value.toFloat * math.pow(2, units.indexOf(unit) * 10)).toLong
    }

  }

  def parseAndRun(args: Seq[String])(runFunc: TestDFSIOConf => Unit): Unit =
    parser.parse(args, TestDFSIOConf()) match {
      case Some(conf) =>
        printOptions(conf)
        runFunc(conf)
      case None => // ignore
    }

  private def printOptions(conf: TestDFSIOConf): Unit = {
    logger.info(s"${TestDFSIOConf.getClass.getSimpleName}.${BuildInfo.version}")
    logger.info("Test mode = {}", conf.mode.command)
    conf.mode match {
      case Write =>
        logger.info(s"outputDir = ${conf.benchmarkDir}")
        logger.info(s"resFile = ${conf.resFileName}")
        logger.info(s"numFiles = ${conf.numFiles}")
        logger.info(s"fileSize = ${conf.fileSize}")
        logger.info(s"bufferSize = ${conf.bufferSize}")
      case Read =>
        logger.info(s"inputDir = ${conf.benchmarkDir}")
        logger.info(s"resFile = ${conf.resFileName}")
        logger.info(s"numFiles = ${conf.numFiles}")
        logger.info(s"fileSize = ${conf.fileSize}")
        logger.info(s"bufferSize = ${conf.bufferSize}")
      case Clean =>
        logger.info("outputDir = {}", conf.benchmarkDir)
      case _ => // ignore
    }
  }

}

