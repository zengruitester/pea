import sbt._


object Dependencies {
  // Compile dependencies

  // format: OFF

  private val akka                           = "com.typesafe.akka"                   %% "akka-actor"                      % "2.6.0"
  private val akkaSlf4j                      = akka.organization                     %% "akka-slf4j"                      % akka.revision
  private val akkaStream                     = akka.organization                     %% "akka-stream"                     % akka.revision
  private val akkaProtobuf                   = akka.organization                     %% "akka-protobuf"                   % akka.revision
  private val akkaTestKit                    = akka.organization                     %% "akka-testkit"                    % akka.revision       % "test"
  
  
  val akkaDependencies = Seq(
    akka,akkaSlf4j,akkaProtobuf,akkaTestKit
  )
}
