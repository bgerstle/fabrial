# fabrial
A server library written in Java 10.

## Getting Started
- [Download](https://www.oracle.com/technetwork/java/javase/downloads/jdk10-downloads-4416644.html) Java 10 JDK
- Import the project into IntelliJ (using the Gradle template)

## Starting the Server
The server can be run via gradle using:

``` shell
./gradlew run
```

Or from IntelliJ by creating an "Application" configuration which targets the main class.

## Running Unit Tests
This project uses JUnit to run tests, which Gradle (the project's task runner) supports with little effort. To run on the command line, use the in-repo gradle wrapper:

``` shell
./gradlew check [-i]
```

Or, from IntelliJ you can create a new JUnit configuration which runs the tests.

## Running Acceptance Tests
First, you'll need `chromedriver` for the tests which verify returned HTML in a browser. Then, run the `testAcceptance` Gradle task:

``` shell
./gradlew testAcceptance
```

## Packaging
The `fatJar` task can be used to create a "fat jar" which contains the server and all of its dependencies. This is also created (and exercised) when running the acceptance tests.

## Etymology
The name comes from a device in the [Stormlight Archives](http://stormlightarchive.wikia.com/wiki/Fabrial) series by Brandan Sanderson. Fabrials harness spiritual energy (stormlight) to accomplish different tasks. For example, [spanreed](http://stormlightarchive.wikia.com/wiki/Spanreed) fabrials harness stormlight in rubies to facilitate communication across large distances. In this case, the server converts raw socket data for the puroses of sending and receiving HTTP messages.
