lazy val sparkBenchmarks = project
  .in(file("."))
  .disablePlugins(AssemblyPlugin)
  .aggregate(dfsio)

lazy val dfsio = project
  .enablePlugins(AutomateHeaderPlugin, AssemblyPlugin, BuildInfoPlugin)
  .settings(
    name := "spark-benchmarks-dfsio",
    buildInfoPackage := "com.bbva.spark.benchmarks.dfsio",
    scalaVersion := "2.10.7",
    scalaHome := Some(file("C:\\tools\\scala-2.10.7")),
    Dependencies.Spark,
    Dependencies.Scopt,
    Dependencies.Alluxio
  )