//#!/bin/scala
package vastblue.file

import java.nio.file.{
  Files => JFiles,
  Path,
//Paths => JPaths,
  CopyOption,
  LinkOption,
  FileStore,
  OpenOption,
  DirectoryStream,
  FileVisitor,
  FileVisitOption
}
import java.util.{Set, List}
import java.nio.file.attribute.*
import java.nio.channels.*
import java.nio.charset.*
import java.io.* // InputStream

object Files {
  // Copies all bytes from an input stream to a file.
  def copy(in: InputStream, target: Path, options: CopyOption*): Long =
    JFiles.copy(in, target, options: _*)
  // Copies all bytes from a file to an output stream.
  def copy(source: Path, out: OutputStream): Long = JFiles.copy(source, out)
  // Copy a file to a target file.
  def copy(source: Path, target: Path, options: CopyOption*): Path =
    JFiles.copy(source, target, options: _*)
  // Creates a directory by creating all nonexistent parent directories first.
  def createDirectories(dir: Path, attrs: FileAttribute[_]*): Path =
    JFiles.createDirectories(dir, attrs: _*)
  // Creates a new directory.
  def createDirectory(dir: Path, attrs: FileAttribute[_]*): Path =
    JFiles.createDirectory(dir, attrs: _*)
  // Creates a new and empty file, failing if the file already exists.
  def createFile(path: Path, attrs: FileAttribute[_]*): Path = JFiles.createFile(path, attrs: _*)
  // Creates a new link (directory entry) for an existing file (optional operation).
  def createLink(link: Path, existing: Path): Path = JFiles.createLink(link, existing)
  // Creates a symbolic link to a target (optional operation).
  def createSymbolicLink(link: Path, target: Path, attrs: FileAttribute[_]*): Path =
    JFiles.createSymbolicLink(link, target, attrs: _*)
  // Creates a new directory in the specified directory, using the given prefix to generate its name.
  def createTempDirectory(dir: Path, prefix: String, attrs: FileAttribute[_]*): Path =
    JFiles.createTempDirectory(dir, prefix, attrs: _*)
  // Creates a new directory in the default temporary-file directory, using the given prefix to generate its name.
  def createTempDirectory(prefix: String, attrs: FileAttribute[_]*): Path =
    JFiles.createTempDirectory(prefix, attrs: _*)
  // Creates a new empty file in the specified directory, using the given prefix and suffix strings to generate its name.
  def createTempFile(dir: Path, prefix: String, suffix: String, attrs: FileAttribute[_]*): Path =
    JFiles.createTempFile(dir, prefix, suffix, attrs: _*)
  // Creates an empty file in the default temporary-file directory, using the given prefix and suffix to generate its name.
  def createTempFile(prefix: String, suffix: String, attrs: FileAttribute[_]*): Path =
    JFiles.createTempFile(prefix, suffix, attrs: _*)
  // Deletes a file.
  def delete(path: Path): Unit = JFiles.delete(path)
  // Deletes a file if it exists.
  def deleteIfExists(path: Path): Boolean = JFiles.deleteIfExists(path)
  // Tests whether a file exists.
  def exists(path: Path, options: LinkOption*): Boolean = JFiles.exists(path, options: _*)
  // Reads the value of a file attribute.
  def getAttribute(path: Path, attribute: String, options: LinkOption*): AnyRef =
    JFiles.getAttribute(path, attribute, options: _*)
  // Returns a file attribute view of a given type.
  def getFileAttributeView[V <: FileAttributeView](
      path: Path,
      `type`: Class[V],
      options: LinkOption*
  ): V = JFiles.getFileAttributeView(path, `type`, options: _*)
  // Returns the FileStore representing the file store where a file is located.
  def getFileStore(path: Path): FileStore = JFiles.getFileStore(path)
  // Returns a file's last modified time.
  def getLastModifiedTime(path: Path, options: LinkOption*): FileTime =
    JFiles.getLastModifiedTime(path, options: _*)
  // Returns the owner of a file.
  def getOwner(path: Path, options: LinkOption*): UserPrincipal = JFiles.getOwner(path, options: _*)
  // Returns a file's POSIX file permissions.
  def getPosixFilePermissions(path: Path, options: LinkOption*): Set[PosixFilePermission] =
    JFiles.getPosixFilePermissions(path, options: _*)
  // Tests whether a file is a directory.
  def isDirectory(path: Path, options: LinkOption*): Boolean = JFiles.isDirectory(path, options: _*)
  // Tests whether a file is executable.
  def isExecutable(path: Path): Boolean = JFiles.isExecutable(path)
  // Tells whether or not a file is considered hidden.
  def isHidden(path: Path): Boolean = JFiles.isHidden(path)
  // Tests whether a file is readable.
  def isReadable(path: Path): Boolean = JFiles.isReadable(path)
  // Tests whether a file is a regular file with opaque content.
  def isRegularFile(path: Path, options: LinkOption*): Boolean =
    JFiles.isRegularFile(path, options: _*)
  // Tests if two paths locate the same file.
  def isSameFile(path1: Path, path2: Path): Boolean = {
    JFiles.isSameFile(path1, path2) // crashes unless both files exist
  }
  // Tests whether a file is a symbolic link.
  def isSymbolicLink(path: Path): Boolean = JFiles.isSymbolicLink(path)
  // Tests whether a file is writable.
  def isWritable(path: Path): Boolean = JFiles.isWritable(path)
  // Move or rename a file to a target file.
  def move(source: Path, target: Path, options: CopyOption*): Path =
    JFiles.move(source, target, options: _*)
  // Opens a file for reading, returning a BufferedReader that may be used to read text from the file in an efficient manner.
  def newBufferedReader(path: Path, cs: Charset): BufferedReader =
    JFiles.newBufferedReader(path, cs)
  // Opens or creates a file for writing, returning a BufferedWriter that may be used to write text to the file in an efficient manner.
  def newBufferedWriter(path: Path, cs: Charset, options: OpenOption*): BufferedWriter =
    JFiles.newBufferedWriter(path, cs, options: _*)
  // Opens or creates a file, returning a seekable byte channel to access the file.
  def newByteChannel(path: Path, options: OpenOption*): SeekableByteChannel =
    JFiles.newByteChannel(path, options: _*)
  // Opens or creates a file, returning a seekable byte channel to access the file.
  def newByteChannel(
      path: Path,
      options: Set[_ <: OpenOption],
      attrs: FileAttribute[_]*
  ): SeekableByteChannel = JFiles.newByteChannel(path, options, attrs: _*)
  // Opens a directory, returning a DirectoryStream to iterate over all entries in the directory.
  def newDirectoryStream(dir: Path): DirectoryStream[Path] = JFiles.newDirectoryStream(dir)
  // Opens a directory, returning a DirectoryStream to iterate over the entries in the directory.
  def newDirectoryStream(
      dir: Path,
      filter: DirectoryStream.Filter[_ >: Path]
  ): DirectoryStream[Path] = JFiles.newDirectoryStream(dir, filter)
  // Opens a directory, returning a DirectoryStream to iterate over the entries in the directory.
  def newDirectoryStream(dir: Path, glob: String): DirectoryStream[Path] =
    JFiles.newDirectoryStream(dir, glob)
  // Opens a file, returning an input stream to read from the file.
  def newInputStream(path: Path, options: OpenOption*): InputStream =
    JFiles.newInputStream(path, options: _*)
  // Opens or creates a file, returning an output stream that may be used to write bytes to the file.
  def newOutputStream(path: Path, options: OpenOption*): OutputStream =
    JFiles.newOutputStream(path, options: _*)
  // Tests whether the file located by this path does not exist.
  def notExists(path: Path, options: LinkOption*): Boolean = JFiles.notExists(path, options: _*)
  // Probes the content type of a file.
  def probeContentType(path: Path): String = JFiles.probeContentType(path)
  // Reads all the bytes from a file.
  def readAllBytes(path: Path): Array[Byte] = JFiles.readAllBytes(path)
  // Read all lines from a file.
  def readAllLines(path: Path, cs: Charset): List[String] = JFiles.readAllLines(path, cs)

  // Reads a file's attributes as a bulk operation.
//def readAttributes[A <: BasicFileAttributes](path: Path, `type`: Class[_ <: A], options: LinkOption *): A = JFiles.readAttributes(path, `type`, options:_*)

  // Reads a set of file attributes as a bulk operation.
//def readAttributes(path: Path, attributes: String, options: LinkOption *): Map[String,AnyRef] = JFiles.readAttributes(path, attributes, options:_*)

  // Reads the target of a symbolic link (optional operation).
  def readSymbolicLink(link: Path): Path = JFiles.readSymbolicLink(link)
  // Sets the value of a file attribute.
  def setAttribute(path: Path, attribute: String, value: AnyRef, options: LinkOption*): Path =
    JFiles.setAttribute(path, attribute, value, options: _*)
  // Updates a file's last modified time attribute.
  def setLastModifiedTime(path: Path, time: FileTime): Path = JFiles.setLastModifiedTime(path, time)
  // Updates the file owner.
  def setOwner(path: Path, owner: UserPrincipal): Path = JFiles.setOwner(path, owner)
  // Sets a file's POSIX permissions.
  def setPosixFilePermissions(path: Path, perms: Set[PosixFilePermission]): Path =
    JFiles.setPosixFilePermissions(path, perms)
  // Returns the size of a file (in bytes).
  def size(path: Path): Long = JFiles.size(path)
  // Walks a file tree.
  def walkFileTree(start: Path, visitor: FileVisitor[_ >: Path]): Path =
    JFiles.walkFileTree(start, visitor)
  // Walks a file tree.
  def walkFileTree(
      start: Path,
      options: Set[FileVisitOption],
      maxDepth: Int,
      visitor: FileVisitor[_ >: Path]
  ): Path = JFiles.walkFileTree(start, options, maxDepth, visitor)
  // Writes bytes to a file.
  def write(path: Path, bytes: Array[Byte], options: OpenOption*): Path =
    JFiles.write(path, bytes.asInstanceOf[Array[Byte]], options: _*)
  // Write lines of text to a file.
  // def write(path: Path, lines: Iterable[_ <: CharSequence], cs: Charset, options: OpenOption *): Path = JFiles.write(path, lines, cs, options:_*)
}
