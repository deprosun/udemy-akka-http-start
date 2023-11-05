# Akka HTTP Learning Project

This repository is part of a learning pathway on Akka HTTP, structured around a Udemy course. It features a series of Scala applications, each designed to illustrate key concepts of the Akka HTTP framework. The applications are grouped into two categories, each focusing on a specific level of server abstraction within Akka HTTP: the low-level and high-level server APIs.

## Project Structure

The codebase is organized into chapters within the `src/main/scala` directory, divided into low-level and high-level server applications.

### High-Level Server Chapters (`part3_highlevelserver`)

- **DirectivesBreakdown.scala**: A detailed exploration of Akka HTTP's directives, showcasing their composition and usage in building routing trees.
- **HighLevelExample.scala**: A practical example implementing a complete service using the high-level API, demonstrating its power and ease of use.
- **HandlingRejections.scala**: Techniques for handling route rejections, including custom rejection handling, to return informative responses to the client.
- **HighLevelExercise.scala**: Interactive exercises to apply the knowledge of the high-level API in hands-on coding challenges.
- **HighLevelIntro.scala**: An introductory guide to the high-level API, setting up a basic server and introducing key concepts.
- **MarshallingJSON.scala**: Instructions on how to marshal and unmarshal JSON payloads using Akka HTTP's integration with Spray JSON.

### Low-Level Server Chapters (`part2_lowlevelserver`)

- **LowLevelHttps.scala**: Configuring an HTTPS server with Akka HTTP's low-level API, with an emphasis on TLS and security.
- **LowLevelRest.scala**: Constructing a RESTful API using the low-level server API for more granular control over request and response handling.
- **LowLevelApi.scala**: A deep dive into Akka HTTP's low-level server API, exploring the building blocks of HTTP request handling.

## Build Configuration

The project is configured with the following `build.sbt`:

```scala
name := "udemy-akka-http"

version := "0.1"

scalaVersion := "2.12.8"

val akkaVersion = "2.5.20"
val akkaHttpVersion = "10.1.7"
val scalaTestVersion = "3.0.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion
)
```

Make sure sbt is installed on your system to manage dependencies and builds.

## Running the Applications

To run an application, use sbt's `runMain` command in the terminal:

```sh
sbt "runMain partX_subpackage.ObjectName"
```

Replace `partX_subpackage.ObjectName` with the actual package and object name for the application you want to run.

## Prerequisites

The following prerequisites are necessary to build and run the applications:

- Scala 2.12.8
- sbt 1.x
- Java JDK 8 or higher

## Contributions

Contributions to this project are welcome. If you have suggestions for improvement or want to add new examples, please fork the repository, make your changes, and submit a pull request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.