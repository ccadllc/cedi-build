sbtPlugin := true

organization := "com.ccadllc.cedi"
name := "build"

crossScalaVersions := Seq(scalaVersion.value)
publishMavenStyle := true

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("org.tpolecat" % "tut-plugin" % "0.6.13")
addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.9.5")
addSbtPlugin("io.crashbox" % "sbt-gpg" % "0.2.1")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.6.4")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.13")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.8.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.3")

licenses += ("Apache 2", url("https://github.com/ccadllc/cedi-build/blob/master/LICENSE"))

organizationHomepage := Some(new URL("http://ccadllc.com"))

unmanagedResources in Compile ++= {
  val base = baseDirectory.value
  (base / "NOTICE") +: (base / "LICENSE") +: (base / "CONTRIBUTING") +: ((base / "licenses") * "LICENSE_*").get
}

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { x => false }
pomExtra := (
  <url>http://github.com/ccadllc/cedi-build</url>
  <scm>
    <url>git@github.com:ccadllc/cedi-build.git</url>
    <connection>scm:git:git@github.com:ccadllc/cedi-build.git</connection>
  </scm>
  <developers>
    <developer>
      <id>mpilquist</id>
      <name>Michael Pilquist</name>
      <url>http://github.com/mpilquist</url>
    </developer>
  </developers>
)

