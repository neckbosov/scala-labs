package lab8

import java.nio.file.{Files, Path, Paths}
import java.util.Comparator

import cats.Id
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.JavaConverters.asScalaIterator
import scala.collection.mutable.ArrayBuffer

class FilesystemTest extends AnyFlatSpec
  with BeforeAndAfter
  with Matchers {
  after {
    val files = Files.walk(Paths.get("tmp"))
    asScalaIterator(files.sorted(Comparator.reverseOrder()).iterator())
      .map(_.toFile).foreach(_.delete())
  }

  "Filesystem" should "work" in {
    val buffer: ArrayBuffer[String] = ArrayBuffer()

    implicit val fs: RealFileSystem[Id] = new RealFileSystem[Id]
    implicit val printer: Printer[Id] = (s: String) => buffer += s
    val program = new Program[Id, Path, Path]
    program.run(Paths.get("tmp"))
    buffer.sorted shouldBe ArrayBuffer("foo", "bar", "baz").sorted
    val files = asScalaIterator(Files.walk(Paths.get("tmp")).iterator()).toList.sorted
    val expected = List("tmp", "tmp/test_dir", "tmp/test_dir/b", "tmp/test_dir/b/bar", "tmp/test_dir/b/baz", "tmp/test_dir/f", "tmp/test_dir/f/foo")
      .map(Paths.get(_)).sorted
    files shouldBe expected
  }
}
