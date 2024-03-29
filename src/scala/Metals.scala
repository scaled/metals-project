//
// Scaled Scala Mode - support for editing Scala code
// https://github.com/scaled/scala-mode/blob/master/LICENSE

package scaled.project

import java.nio.file.{Files, Path}
import scaled._
import scaled.pacman.JDK

object Metals {

  val ProjectFile = ".bloop"

  @Plugin(tag="project-root")
  class MetalsRootPlugin extends RootPlugin.Directory(ProjectFile)

  @Plugin(tag="langserver")
  class MetalsLangPlugin extends LangPlugin {
    def suffs (root :Project.Root) = Set("scala")
    def canActivate (root :Project.Root) = Files.exists(root.path.resolve(ProjectFile))
    def createClient (proj :Project) = Future.success(
      new MetalsLangClient(proj.metaSvc, proj.root))
  }

  def serverCmd (metaSvc :MetaService, root :Project.Root) = {
    // our package contains the Metals server code as a dependency, so we obtain it from there;
    // this is a modest hack since we don't need the code to be loadable from Scaled, but it won't
    // be loaded, so this is just a simple way to ensure that it's slurped down from Maven Central
    // at the time that we need it
    val pkgSvc = metaSvc.service[PackageService]
    val pkgSource = "git:https://github.com/scaled/metals-project.git"
    val pkgCP = pkgSvc.classpath(pkgSource).mkString(System.getProperty("path.separator"))
    val langMain = "scala.meta.metals.Main"

    val projJdk = JDK.jdks.find(jdk => Files.exists(jdk.binJava)) getOrElse JDK.thisJDK
    Seq(projJdk.binJava.toString, "-classpath", pkgCP, langMain)
  }
}

class MetalsLangClient (msvc :MetaService, root :Project.Root)
    extends LangClient(msvc, root.path, Metals.serverCmd(msvc, root)) {

  override def name = "Metals"

  // override def format (buffer :Buffer, wrapWidth :Int, text :String) =
  //   format(buffer, text, "source.scala")

  // // do great violence to type signatures to provide a terse summary
  // override def formatSig (rawSig :String) :LineV = {
  //   var sig = Filler.flatten(rawSig)
  //   def skipPastNext (sig :String, c :Char, start :Int) = {
  //     var brackets = 0 ; var parens = 0 ; var ii = start
  //     while (ii < sig.length && (brackets > 0 || parens > 0 || sig.charAt(ii) != c)) {
  //       val c = sig.charAt(ii)
  //       if (c == '[') brackets += 1
  //       if (c == '(') parens += 1
  //       if (c == ']') brackets -= 1
  //       if (c == ')') parens -= 1
  //       ii += 1
  //     }
  //     ii + 1
  //   }
  //   // strip off the type parameters
  //   if (sig.charAt(0) == '[') sig = sig.substring(skipPastNext(sig, ']', 1))
  //   // strip off the implicit argument list
  //   val impstart = sig.indexOf("(implicit")
  //   if (impstart >= 0) {
  //     val impend = skipPastNext(sig, ')', impstart+1)
  //     sig = sig.substring(0, impstart) + sig.substring(impend)
  //   }
  //   // strip off qualifiers from types
  //   def stripQuals (sig :String) :String = {
  //     val stripped = sig.replaceAll("""\w+\.""", "")
  //     if (stripped == sig) sig
  //     else stripQuals(stripped)
  //   }
  //   Line(stripQuals(sig))
  // }
}
