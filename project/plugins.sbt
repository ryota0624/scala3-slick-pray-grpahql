addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.5")
addSbtPlugin("com.github.tototoshi" % "sbt-slick-codegen" % "2.2.0")
addSbtPlugin("com.github.ghostdogpr" % "caliban-codegen-sbt" % "2.9.0")

libraryDependencies += "com.typesafe.slick" %% "slick-codegen" % "3.5.1"
libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.46.0.0"
