package org.stummi.maven.executable.archiveBuilder;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ArchiveBuildingFileVisitor implements FileVisitor<Path> {
	private final ArchiveBuilder builder;
	private final String prefix;
	private final Path path;

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		String target = addPrefix(dir);

		if (!target.isEmpty()) {
			// root directory for empty prefix
			builder.putDirectory(target);
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		String target = addPrefix(file);
		builder.putPhysicalFile(target, file);
		return FileVisitResult.CONTINUE;
	}

	private String addPrefix(Path rel) {
		if (rel.equals(path)) {
			return prefix;
		}

		return prefix + "/" + path.relativize(rel);
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		throw new IOException("visit file failed", exc);
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		if (exc != null) {
			throw new IOException("visit directory failed", exc);
		}
		return FileVisitResult.CONTINUE;
	}

}
