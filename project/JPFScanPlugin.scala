import javax.xml.parsers.SAXParserFactory

import scala.collection.JavaConversions._
import org.jdom2.input.SAXBuilder
import org.jdom2.input.sax.XMLReaders
import sbt._
import sbt.Keys._
import JPFPlugin.autoImport._

import scala.annotation.tailrec
import Common._

object JPFScanPlugin extends AutoPlugin {
  val dirs = Seq("Source/Plugins", "Platform/Plugins", "Interface/Plugins")
  val parentRef = LocalProject("equellaServer")

  case class ParsedJPF(baseDir: File, id: String, internalDeps: Set[(String, Boolean)], externalDeps: Set[(String, Boolean)])


  def parseJPF(f: File): ParsedJPF = {
    val x = saxBuilder.build(f)
    val root = x.getRootElement
    val pluginId = root.getAttribute("id").getValue
    val (extDeps, deps) = root.getChildren("requires").flatMap(_.getChildren("import")).map { e =>
      (e.getAttributeValue("plugin-id"), e.getAttributeValue("exported", "false") == "true")
    }.partition(_._1.contains(":"))

    ParsedJPF(f.getParentFile, pluginId, deps.toSet, extDeps.toSet)
  }

  def toLocalProject(pluginId: String) = LocalProject(toSbtPrj(pluginId))

  def classpathDep(pluginId: String): ClasspathDep[ProjectReference] = {
    ClasspathDependency(toLocalProject(pluginId), None)
  }

  def convertAll(parsedMap: Map[String, ParsedJPF], already: Set[String],
                 processed: List[Project], pId: Iterable[String]): (Set[String], List[Project]) = {
    pId.foldLeft((already, processed)) {
      case ((a, p), c) => convertOne(parsedMap, a, p, c)
    }
  }

  @tailrec
  def depsWithExports(d: Set[String], parsedMap: Map[String, ParsedJPF], added: Set[String]): Set[String] = {
    val newDeps = d &~ added
    if (newDeps.isEmpty) added else {
      val exportedNew = newDeps.flatMap(s => parsedMap.get(s).map(_.internalDeps.filter(_._2).map(_._1)).getOrElse(Set.empty))
      depsWithExports(exportedNew, parsedMap, added ++ newDeps)
    }
  }

  def convertOne(parsedMap: Map[String, ParsedJPF], already: Set[String],
                 processed: List[Project], pId: String): (Set[String], List[Project]) = {
    if (already.contains(pId)) (already, processed) else {
      parsedMap.get(pId).map {
        case ParsedJPF(baseDir, _, internalDeps, _) =>
          val deps = internalDeps.map(_._1)
          val (a, l) = convertAll(parsedMap, already + pId, processed, deps)
          val sbtFile = Option(baseDir / "build.sbt").filter(_.exists)
          val prjDeps = deps.toSeq.map(classpathDep)
          val prj = Project(toSbtPrj(pId), baseDir, dependencies = prjDeps)
            .settings(
              managedClasspath in Compile ++= (managedClasspath in (parentRef, Compile)).value,
              managedClasspath in Compile ++= {
                jpfLibraryJars.all(ScopeFilter(inProjects(depsWithExports(deps, parsedMap, Set.empty).map(toLocalProject).toSeq: _*))).value.flatten
              }
            )
            .enablePlugins(JPFPlugin)
            .addSbtFiles(sbtFile.toSeq : _*)
          (a, prj :: l)
      }.getOrElse {
        System.err.println(s"Could not find plugin for id $pId")
        (already, processed)
      }
    }
  }

  override def trigger = noTrigger

  override def derivedProjects(proj: ProjectDefinition[_]): Seq[Project] = {
    val baseDir = proj.base
    val allManifests = dirs.foldLeft(Seq.empty[File])((m, dir) => ((baseDir / dir) ** "plugin-jpf.xml").get ++ m)
    val manifestMap = allManifests.map(parseJPF).map(p => (p.id, p)).toMap
    val pluginList = manifestMap.keys
//    val pluginList = Seq("com.tle.common.remoterepo.z3950", "com.tle.core.application")
    val (_, projects) = convertAll(manifestMap, Set.empty, Nil, pluginList)
    val allPlugins = Project("allPlugins", baseDir / "Source/Plugins").aggregate(projects.map(Project.projectToRef): _*)
    allPlugins +: projects
  }
}