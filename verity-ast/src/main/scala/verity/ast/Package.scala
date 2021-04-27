package verity.ast

import verity.ast.*
import verity.parsing.*
// import verity.parsing.GetText.given GetText
import verity.ast.infile.*

import com.typesafe.scalalogging.Logger

import java.io.File
import scala.annotation.{tailrec, targetName}
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Future, ExecutionContext}

sealed trait Package extends NamedTree {
  def name: String
  def subPkgs: ListBuffer[PkgNode]
  def files: ListBuffer[FileNode]

  def classlikes: Iterable[Classlike] = files.view.flatMap(_.classlikes)

  /** Find a class directly in this package (not in a subpackage) by the given name.
    */
  def findClasslike(className: String): Option[Classlike] =
    this.classlikes.find(_.name.toString == className)

  def walk(f: FileNode => (RootPkg, Logger) ?=> Unit)(using RootPkg, Logger): Unit = {
    this.subPkgs.foreach { p => Future(p.walk(f))(ExecutionContext.global) }

    this.files.foreach(f)
  }

  protected def walkWithPath(
      parents: List[Package],
      path: String,
      f: (FileNode, List[Package], String) => (RootPkg, Logger) ?=> Unit
  )(using RootPkg, Logger): Unit = {
    val newParents = this :: parents
    val newPath = path + this.name
    this.subPkgs.foreach { p =>
      Future(p.walkWithPath(newParents, newPath, f))(ExecutionContext.global)
    }

    this.files.foreach(f(_, newParents, newPath))
  }

  /** Recursively walk down a package and all its subpackages, and for every
    * file encountered, run the given function on that file and the recorded list of packages (reverse order),
    * and the path (as a string).
    * @param f Execute on a file, also given the list of packages the file is in (in reverse) and the file's path
    */
  def walkWithPath(
      f: (FileNode, List[Package], String) => (RootPkg, Logger) ?=> Unit
  )(using RootPkg, Logger): Unit = {
    val parents = this.parents
    this.walkWithPath(parents, Package.canonicalName(this.name, parents), f)
  }

  def parents: List[Package]

  override def toString = s"package $name"
}

case class RootPkg(subPkgs: ListBuffer[PkgNode], files: ListBuffer[FileNode]) extends Package {
  def name = ""
  def parents = Nil
}

//TODO does the parent have to be stored?
case class PkgNode(
    name: String,
    subPkgs: ListBuffer[PkgNode],
    files: ListBuffer[FileNode],
    parent: Package
) extends Package {
  def parents = parent :: parent.parents
}

object Package {
  type Importable = (Package | Classlike | MethodGroup | Method | Field | EnumConstant) & NamedTree
  type ImportParent = Package | Classlike

  /** Find a subpackage given the relative path of the package. The last found subpackage and the remaining path are returned.
    * @param pkgPath The path of a subpackage or class or other tree somewhere inside this package.
    *                The Iterable is *not* in reverse order.
    */
  @tailrec def findPkgRel(
      pkg: Package,
      pkgPath: Iterable[String]
  ): (Package, Iterable[String]) =
    if (pkgPath.isEmpty) {
      (pkg, pkgPath)
    } else {
      val subName = pkgPath.head

      pkg.subPkgs.find(_.name == subName) match {
        case Some(subPkg) => findPkgRel(subPkg, pkgPath.tail)
        case None         => (pkg, pkgPath)
      }
    }

  /** Find a subpackage/class/method/field given the relative path of the tree.
    * @param path The path of a subpackage or class or other tree somewhere inside this package.
    *                The Iterable is *not* in reverse order.
    */
  def findImptableRel(pkg: Package, path: Iterable[String]): Option[Importable] =
    if (path.isEmpty) {
      Some(pkg)
    } else {
      val subName = path.head
      val rest = path.tail

      pkg.subPkgs.view
        .find(_.name == subName)
        .flatMap(findImptableRel(_, rest)).asInstanceOf[Option[Package.Importable]]
        .orElse(
            findCls(pkg, subName)
              .flatMap(_.findMember(rest).asInstanceOf[Option[Package.Importable]]: Option[Package.Importable]): Option[Package.Importable]
        ): Option[Package.Importable]
    }

  /** Find a subpackage given the absolute path of the package. The last found subpackage and the remaining path are returned.
    * @param pkgPath The path of a subpackage or class or other tree somewhere inside this package.
    *                The Iterable is *not* in reverse order.
    */
  def findPkgPAbs(pkgPath: Iterable[String])(using root: RootPkg): (Package, Iterable[String]) =
    findPkgRel(root, pkgPath)

  def findImptableAbs(path: Iterable[String])(using root: RootPkg): Option[Importable] =
    findImptableRel(root, path)

  /** Find a class with the given name directly in this package (not in a subpackage)
    */
  def findCls(pkg: Package, clsName: String): Option[Classlike] =
    pkg.classlikes.find(_.name.toString == clsName)

  private def canonicalName(pkgName: String, parents: Iterable[Package]) =
    parents.foldLeft(pkgName)((endName, parentName) => s"$parentName.$endName")
}

