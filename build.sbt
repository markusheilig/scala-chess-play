name := "scala_chess_play"

version := "1.0"

//lazy val scalaChess = RootProject(uri("git://github.com/scala-chess/scala-chess-api.git#master"))
lazy val scalaChessApi = RootProject(file("../scala-chess-api"))

lazy val `scala_chess_play` = (project in file(".")).enablePlugins(PlayScala).dependsOn(scalaChessApi)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq( jdbc , cache , ws , specs2 % Test )

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.akka" % "akka-remote_2.11" % "2.4.12"

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"



