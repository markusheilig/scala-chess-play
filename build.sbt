name := "scala_chess_play"

version := "1.0"

lazy val scalaChess = RootProject(uri("git://github.com/markusheilig/scala-chess.git#master"))

lazy val `scala_chess_play` = (project in file(".")).enablePlugins(PlayScala).dependsOn(scalaChess)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq( jdbc , cache , ws   , specs2 % Test )

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"