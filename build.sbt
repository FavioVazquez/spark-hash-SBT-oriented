name := "spark-hash-SBT-oriented"

version := "0.1.5"

scalaVersion := "2.10.4"

unmanagedBase := baseDirectory.value / "lib"

libraryDependencies ++= Seq(
//  "org.apache.spark" % "spark-core_2.10" % "1.3.1" % "provided"
//  "org.apache.spark" %% "spark-sql"  % "1.3.1" % "provided",
"org.apache.logging.log4j" % "log4j-core" % "2.2",
"org.apache.logging.log4j" % "log4j-api" % "2.2",
"junit" % "junit" % "4.12"
)

assemblyJarName in assembly := "spark-hash.jar"

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))
