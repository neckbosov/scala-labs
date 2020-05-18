package lab8

import java.nio.file.{Files, Path, Paths}

import cats.instances.list.catsStdInstancesForList
import cats.syntax.all._
import cats.{Applicative, Id, Monad}

import scala.collection.JavaConverters.asScalaIterator
import scala.language.higherKinds

trait MkDir[F[_], Dir] {
  def mkDir(dir: Dir, name: String): F[Dir]
}

trait MkFile[F[_], Dir, File] {
  def mkFile(dir: Dir, name: String): F[File]
}

trait DirFiles[F[_], Dir, File] {
  def getFiles(dir: Dir): F[List[File]]
}

trait FileName[F[_], File] {
  def getName(file: File): F[String]
}

trait Printer[F[_]] {
  def print(s: String): F[Unit]
}

trait MoveFile[F[_], File] {
  def move(source: File, target: File): F[File]
}

trait DirFoldable[F[_], Dir, File] {
  def foldMap[R](dir: Dir)(f: File => F[R]): F[List[R]]
}

trait CreatePathToFile[F[_], Dir, File] extends FileName[F, File] {
  def createPath(dir: Dir, name: String): F[File]
}

class Program[F[_], Dir, File](implicit
                               F: Monad[F],
                               mkDir: MkDir[F, Dir],
                               mkFile: MkFile[F, Dir, File],
                               fileName: FileName[F, File],
                               printer: Printer[F],
                               moveFile: MoveFile[F, File],
                               fileFolder: DirFoldable[F, Dir, File],
                               dirFiles: DirFiles[F, Dir, File],
                               pathsCreator: CreatePathToFile[F, Dir, File]) {
  def run(dir: Dir): F[Unit] = for {
    testDir <- mkDir.mkDir(dir, "test_dir")
    _ <- List("foo", "bar", "baz").traverse(mkFile.mkFile(testDir, _))
    files <- dirFiles.getFiles(testDir)
    filenames <- files.traverse(fileName.getName)
    _ <- filenames.traverse(printer.print)
    firstLetters <- fileFolder.foldMap(testDir)(file => fileName.getName(file).map(s => s.charAt(0)))
    firstLetterDirs <- firstLetters.traverse(c => mkDir.mkDir(testDir, c.toString))
    newFiles <- firstLetterDirs.zip(filenames).traverse { case (dir, filename) => pathsCreator.createPath(dir, filename) }
    _ <- files.zip(newFiles).traverse { case (source, target) => moveFile.move(source, target) }
  } yield ()
}

class RealFileSystem[F[_] : Applicative] extends MkDir[F, Path]
  with MkFile[F, Path, Path]
  with MoveFile[F, Path]
  with DirFoldable[F, Path, Path]
  with FileName[F, Path]
  with DirFiles[F, Path, Path]
  with CreatePathToFile[F, Path, Path] {
  override def mkDir(dir: Path, name: String): F[Path] =
    Files.createDirectories(dir.resolve(name)).pure[F]

  override def mkFile(dir: Path, name: String): F[Path] =
    Files.createFile(dir.resolve(name)).pure[F]

  override def move(source: Path, target: Path): F[Path] = Files.move(source, target).pure[F]

  private def getDirEntry(dir: Path): List[Path] = asScalaIterator(Files.walk(dir).iterator()).filter(_ != dir).toList

  override def foldMap[R](dir: Path)(f: Path => F[R]): F[List[R]] =
    getDirEntry(dir).filter(p => !Files.isDirectory(p)).traverse(f)

  override def getName(file: Path): F[String] = file.getFileName.toString.pure[F]

  override def getFiles(dir: Path): F[List[Path]] = foldMap(dir)(_.pure[F])

  override def createPath(dir: Path, name: String): F[Path] = dir.resolve(name).pure[F]
}

class ConsolePrinter[F[_] : Applicative] extends Printer[F] {
  override def print(s: String): F[Unit] = println(s).pure[F]
}

object TypeClasses {

  def main(args: Array[String]): Unit = {
    implicit val fs: RealFileSystem[Id] = new RealFileSystem[Id]
    implicit val printer: ConsolePrinter[Id] = new ConsolePrinter[Id]
    val program = new Program[Id, Path, Path]

    program.run(Paths.get("."))

  }
}