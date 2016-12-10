sbtPlugin := true

organization := "com.ccadllc.cedi"
name := "build"

crossScalaVersions := Seq(scalaVersion.value)
publishMavenStyle := true

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")
addSbtPlugin("org.tpolecat" % "tut-plugin" % "0.4.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.9.1")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.11")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

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

useGpg := true
useGpgAgent := true

releasePublishArtifactsAction := PgpKeys.publishSigned.value

