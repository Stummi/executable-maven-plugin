package org.stummi.maven.executable.archiveBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

public interface ArchiveBuilder extends Closeable {

	/**
	 * creates a new Directory in the archive
	 *
	 * @throws IOException
	 */
	void putDirectory(String name) throws IOException;

	/**
	 * puts a file into the archive
	 * 
	 * @param name
	 *            The name the file should have in the archive
	 * @param size
	 *            The size of the files content
	 * @param is
	 *            The InputStream the files content should be read from
	 * @param executable
	 *            Mark the file as executable if possible
	 */
	void putFile(String name, long size, InputStream is, boolean executable) throws IOException;

	/**
	 * Recursively puts a physical existing directory in the archive
	 * 
	 * @param destInArchive
	 *            the root path the directory gets within the archive.
	 * 
	 * @param path
	 *            the physical path to put into the archive
	 */
	default void putPhysicalDirectoryRecursive(String destInArchive, Path path) throws IOException {
		Files.walkFileTree(path, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
				new ArchiveBuildingFileVisitor(this, destInArchive, path));
	}

	/**
	 * Puts a physical existing file in the archive
	 * 
	 * @param name
	 *            The name which the file should get in the archive
	 * @param path
	 *            the file to put into the archive
	 * 
	 */
	void putPhysicalFile(String name, Path path) throws IOException;

}
