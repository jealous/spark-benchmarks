import sbt._
import Keys._

object Dependencies {

  val AlluxioVersion = "1.4.0"
  val ScalaVersion = "2.10.7"
  val ScalaLoggingVersion = "1.1.0"
  val ScalaTestVersion = "3.0.1"
  val ScoptVersion = "3.5.0"
  val SparkVersion = "2.1.0"

  val Common = Seq(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
      "com.typesafe" %% "scalalogging-slf4j" % ScalaLoggingVersion
    )
  )

  val Spark = Seq(
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % SparkVersion % Provided,
      "org.apache.spark" %% "spark-sql" % SparkVersion % Provided
    )
  )

  val SparkMllib = Seq(
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-mllib" % SparkVersion % Provided
    )
  )

  val Alluxio = Seq(
    libraryDependencies ++= Seq(
      "org.alluxio" % "alluxio-core-client" % AlluxioVersion % Provided
    )
  )

  val Scopt = Seq(
    libraryDependencies ++= Seq(
      "com.github.scopt" %% "scopt" % ScoptVersion
    )
  )

}