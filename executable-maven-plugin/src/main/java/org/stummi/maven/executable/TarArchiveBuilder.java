package org.stummi.maven.executable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;

import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarHeader;
import org.kamranzafar.jtar.TarOutputStream;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TarArchiveBuilder implements AutoCloseable {
	@RequiredArgsConstructor
	public class TarArchiveWalker implements FileVisitor<Path> {
		private final String prefix;
		private final Path path;

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			String target = prefix + "/" + path.relativize(dir);
			putDirectory(target);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			String target = prefix + "/" + path.relativize(file);
			putPhysicalFile(target, file.toFile());
			return FileVisitResult.CONTINUE;
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

	private final TarOutputStream tos;

	public TarArchiveBuilder(OutputStream os) {
		this(os instanceof TarOutputStream ? (TarOutputStream) os : new TarOutputStream(os));
	}

	public void addDirectoryRecursive(String prefix, File file) throws IOException {
		Path path = file.toPath();

		Files.walkFileTree(path, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new TarArchiveWalker(prefix, path));
	}

	public void putDirectory(String name) throws IOException {
		TarHeader th = TarHeader.createHeader(name, 0, System.currentTimeMillis() / 1000, true, 0755);
		addUserInfo(th);
		TarEntry te = new TarEntry(th);
		tos.putNextEntry(te);
	}

	private void addUserInfo(TarHeader th) {
		th.userId = 1000;
		th.groupId = 1000;
		th.userName = new StringBuffer();
		th.groupName = new StringBuffer();
	}

	public void putPhysicalFile(String name, File file) throws IOException {
		TarEntry entry = new TarEntry(file, name);
		addUserInfo(entry.getHeader());
		tos.putNextEntry(entry);

		try (FileInputStream fis = new FileInputStream(file)) {
			byte[] buffer = new byte[2048];
			int readLen;
			while ((readLen = fis.read(buffer)) > 0) {
				tos.write(buffer, 0, readLen);
			}
		}
	}

	@Override
	public void close() throws IOException {
		tos.close();
	}

}
