/*
 * Copyright 2016 Combined Conditional Access Development, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ccadllc.cedi.build

import sbt._
import Keys._

import com.typesafe.sbt.SbtGit._
import GitKeys._

import com.typesafe.sbt.osgi.SbtOsgi
import SbtOsgi.autoImport._

import com.typesafe.sbt.SbtPgp
import SbtPgp.autoImport._

import sbtrelease.ReleasePlugin.autoImport._

import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys._

import org.scalastyle.sbt.ScalastylePlugin

import java.net.URL

object BuildSettings extends AutoPlugin {

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  object autoImport {

    case class Contributor(githubUsername: String, name: String)
    lazy val contributors = settingKey[Seq[Contributor]]("Contributors to the project")
    lazy val githubProject = settingKey[String]("Name of the github project for this repository")
    lazy val githubHttpUrl = settingKey[String]("HTTP URL to the github repository")
    lazy val rootPackage = settingKey[String]("Root package of the project")
    lazy val docSourcePath = settingKey[File]("Path to pass as -sourcepath argument to ScalaDoc")
    lazy val runScalastyle = taskKey[Unit]("Runs scalastyle on main sources")

    def buildOsgiBundle(rootPackage: String): Seq[Setting[_]] = {
      (autoImport.rootPackage := rootPackage) ++ BuildSettings.osgiSettings
    }

    lazy val emptyValue: Unit = ()

    lazy val noPublish = Seq(
      publish := emptyValue,
      publishLocal := emptyValue,
      PgpKeys.publishSigned := emptyValue,
      publishArtifact := false
    )
  }
  import autoImport._

  private def baseSettings = Seq(
    organization := "com.ccadllc.cedi",
    organizationHomepage := Some(new URL("http://ccadllc.com")),
    githubHttpUrl := s"https://github.com/ccadllc/${githubProject.value}/",
    git.remoteRepo := "git@github.com:ccadllc/${githubProject.value}.git",
    licenses += ("Apache 2", url(githubHttpUrl.value + "blob/master/LICENSE")),
    contributors := Seq.empty,
    unmanagedResources in Compile ++= {
      val base = baseDirectory.value
      (base / "NOTICE") +: (base / "LICENSE") +: (base / "CONTRIBUTING") +: ((base / "licenses") * "LICENSE_*").get
    },
    resolvers += Resolver.sonatypeRepo("public")
  )

  private def scalaSettings = Seq(
    scalaVersion := crossScalaVersions.value.head,
    crossScalaVersions := Seq("2.12.6", "2.11.12", "2.13.0-M4"),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked",
      "-Xlint",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Ywarn-unused-import",
      "-Xfuture"
    ) ++ (CrossVersion.partialVersion(scalaBinaryVersion.value) match {
      case Some((2, v)) if v <= 12 => Seq("-Ypartial-unification", "-Yno-adapted-args")
      case other => Seq.empty
    }),
    docSourcePath := baseDirectory.value,
    scalacOptions in (Compile, doc) ++= {
      val tagOrBranch = {
        if (version.value endsWith "SNAPSHOT") gitCurrentBranch.value
        else ("v" + version.value)
      }
      val options = Seq(
        "-implicits",
        "-implicits-show-all",
        "-sourcepath", docSourcePath.value.getCanonicalPath,
        "-doc-source-url", githubHttpUrl.value + "tree/" + tagOrBranch + "€{FILE_PATH}.scala"
      )
      if (!scalaBinaryVersion.value.startsWith("2.10")) "-diagrams" +: options else options
    },
    scalacOptions in (Compile, console) ~= { _ filterNot { o => o == "-Ywarn-unused-import" || o == "-Xfatal-warnings" } },
    scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,
    testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD")
  )

  private def osgiSettings = SbtOsgi.autoImport.osgiSettings ++ Seq(
    OsgiKeys.exportPackage := Seq(rootPackage.value + ".*;version=${Bundle-Version}"),
    OsgiKeys.importPackage := Seq("""scala.*;version="${range;[==,=+)}"""", "*"),
    OsgiKeys.privatePackage := Seq(rootPackage.value + ".*"),
    OsgiKeys.additionalHeaders := Map("-removeheaders" -> "Include-Resource,Private-Package")
  )

  private def publishingSettings = Seq(
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (version.value.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { x => false },
    pomExtra := (
      <url>http://github.com/ccadllc/{githubProject.value}</url>
      <scm>
        <url>${git.remoteRepo.value}</url>
        <connection>scm:git:${git.remoteRepo.value}</connection>
      </scm>
      <developers>
        {for (Contributor(username, name) <- contributors.value) yield
        <developer>
          <id>{username}</id>
          <name>{name}</name>
          <url>https://github.com/{username}</url>
        </developer>
        }
      </developers>
    ),
    pomPostProcess := { (node) =>
      import scala.xml._
      import scala.xml.transform._
      def stripIf(f: Node => Boolean) = new RewriteRule {
        override def transform(n: Node) =
          if (f(n)) NodeSeq.Empty else n
      }
      val stripTestScope = stripIf { n => n.label == "dependency" && (n \ "scope").text == "test" }
      new RuleTransformer(stripTestScope).transform(node)(0)
    },
    useGpg := true,
    useGpgAgent := true
  )

  private def releaseSettings = Seq(
    releaseCrossBuild := true,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value
  )

  private def mimaSettings = mimaDefaultSettings ++ Seq(
    mimaPreviousArtifacts := previousVersion(version.value).map { pv =>
      organization.value % (normalizedName.value + "_" + scalaBinaryVersion.value) % pv
    }.toSet
  )

  private def previousVersion(currentVersion: String): Option[String] = {
    val Version = """(\d+)\.(\d+)\.(\d+).*""".r
    val Version(x, y, z) = currentVersion
    if (z == "0") None
    else Some(s"$x.$y.${z.toInt - 1}")
  }

  private def scalastyleSettings = Seq(
    (org.scalastyle.sbt.ScalastylePlugin.autoImport.scalastyleConfigUrl in Compile) := Some(this.getClass.getResource("/com/ccadllc/cedi/build/scalastyle-config.xml")),
    runScalastyle := org.scalastyle.sbt.ScalastylePlugin.autoImport.scalastyle.in(Compile).toTask("").value,
    (test in Test) := ((test in Test).dependsOn(runScalastyle)).value
  )

  override def projectSettings = {
    baseSettings ++ scalaSettings ++ publishingSettings ++ releaseSettings ++ mimaSettings ++ scalastyleSettings
  }
}
