# Akka HTTP Learning Project

This repository is part of my learning journey through a Udemy course on Akka HTTP. It contains a suite of applications, each corresponding to a specific concept within the Akka HTTP framework. These applications are implemented in Scala and organized into distinct parts focusing on both the high-level and low-level server APIs provided by Akka HTTP.

## Project Structure

Applications are categorized into `part2_lowlevelserver` and `part3_highlevelserver`, representing the modules within the course:

- `part2_lowlevelserver`: Applications demonstrating the low-level server API for detailed HTTP handling.
- `part3_highlevelserver`: Applications exploring the high-level server API for building web services with Akka HTTP's routing DSL.

```plaintext
- src/
  - main/
    - scala/
      - part2_lowlevelserver/
        - LowLevelApi.scala
        - LowLevelHttps.scala
        - LowLevelRest.scala
      - part3_highlevelserver/
        - DirectivesBreakdown.scala
        - HandlingRejections.scala
        - HighLevelExample.scala
        - HighLevelExercise.scala
        - HighLevelIntro.scala
        - MarshallingJSON.scala
- README.md
- build.sbt
```

## Build Configuration

The `build.sbt` file is configured with the following settings and dependencies:

```scala
name := "udemy-akka-http"

version := "0.1"

scalaVersion := "2.12.8"

val akkaVersion = "2.5.20"
val akkaHttpVersion = "10.1.7"
val scalaTestVersion = "3.0.5"

libraryDependencies ++= Seq(
  // Akka Streams
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  // Akka HTTP Core
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  // Akka HTTP Spray JSON for JSON marshalling
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  // Akka HTTP TestKit for testing
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
  // Akka TestKit for Actor testing
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  // ScalaTest for general testing
  "org.scalatest" %% "scalatest" % scalaTestVersion
)
```

Make sure you have sbt installed on your machine to manage project dependencies and builds.

## Running the Applications

To execute an application, run the following command in your terminal from the project root:

```sh
sbt "runMain partX_subpackage.ObjectName"
```

Replace `partX_subpackage.ObjectName` with the specific application's package and object name you wish to run. For example:

```sh
sbt "runMain part3_highlevelserver.DirectivesBreakdown"
```

## Prerequisites

To work on this project, you will need:

- Scala 2.12.8 or higher
- sbt 1.x or higher
- Java JDK 8 or higher

## Contributions

I am open to suggestions and contributions that help improve the examples provided. If you have insights or enhancements, please fork the repository, make your changes, and create a pull request.

## License

The contents of this project are covered under the MIT License.

---

Make sure to save this README in the root of your project directory. Adjust any sections as needed to fit the actual configuration and usage of your project.