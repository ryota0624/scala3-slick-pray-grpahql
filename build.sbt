import _root_.caliban.tools.Codegen
import _root_.slick.codegen.SourceCodeGenerator
import sbt.Compile

name := """blog"""

(ThisBuild / version) := "1.0-SNAPSHOT"
(ThisBuild / scalaVersion) := "3.5.1"
(ThisBuild / libraryDependencies) ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test
)
(ThisBuild / scalacOptions) ++= Seq("-Xmax-inlines", "64")

val circeVersion = "0.14.1"

lazy val root = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      "com.dripower" %% "play-circe" % "3014.1",
      "org.playframework" %% "play-slick" % "6.1.1",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.5.1",
      "org.xerial" % "sqlite-jdbc" % "3.46.0.0",
      "com.github.ghostdogpr" %% "caliban" % "2.9.0",
      "com.github.ghostdogpr" %% "caliban-tapir" % "2.9.0",
      "com.github.ghostdogpr" %% "caliban-play" % "2.9.0"
    ) ++ Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion)
  )
  .enablePlugins(PlayScala)
  .aggregate(database)
  .dependsOn(database)
  .aggregate(graphql)
  .dependsOn(graphql)
// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"

lazy val database = (project in file("modules/database"))
  .enablePlugins(CodegenPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick" % "3.5.2"
    ),
    slickCodegenDatabaseUrl := "jdbc:sqlite:~/git/scala/blog/students.db",
    slickCodegenDriver := _root_.slick.jdbc.SQLiteProfile,
    slickCodegenJdbcDriver := "org.sqlite.JDBC",
    slickCodegenOutputPackage := "generated.database",
    slickCodegenCodeGenerator := { (model: slick.model.Model) =>
      new SourceCodeGenerator(model) {
        override def Table = new Table(_) {
          override def Column = new Column(_) {
            override def rawType = this.model.tpe match {
              case "java.sql.Timestamp" =>
                "java.time.Instant" // kill j.s.Timestamp
              case _ =>
                super.rawType
            }
          }
        }
      }
    },
    (Compile / sourceGenerators) += slickCodegen.taskValue
  )

lazy val graphql = (project in file("modules/grqphql"))
  // enable caliban codegen plugin
  .enablePlugins(CalibanPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.ghostdogpr" %% "caliban" % "2.9.0"
    ),
//    // add code generation settings
    Compile / caliban / calibanSettings := Seq(
      calibanSetting(file("src/main/graphql/tweet.graphql"))(
        // important to set this, otherwise you'll get client code
        _.genType(Codegen.GenType.Schema)
          // you can customize the codegen further with this DSL
          .packageName("graphql.tweet")
          .clientName("TweetSchema")
          .scalarMapping(
            ("Date", "java.time.LocalDate"),
            ("Url", "String"),
            ("ID", "String")
          )
      )
    )
  )
