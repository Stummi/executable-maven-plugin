package org.stummi.maven.executable.archiveBuilder;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarHeader;
import org.kamranzafar.jtar.TarOutputStream;

import lombok.RequiredArgsConstructor;

/**
 * High level abstraction of {@link TarOutputStream}
 */
@RequiredArgsConstructor
public class TarArchiveBuilder implements ArchiveBuilder {

	private final TarOutputStream tos;
	private final UserData userData;

	public TarArchiveBuilder(OutputStream os, UserData data) {
		this(os instanceof TarOutputStream ? (TarOutputStream) os : new TarOutputStream(os), data);
	}

	@Override
	public void putDirectory(String name) throws IOException {
		TarHeader th = TarHeader.createHeader(name, 0, System.currentTimeMillis() / 1000, true, 0755);
		addUserInfo(th);
		TarEntry te = new TarEntry(th);
		tos.putNextEntry(te);
	}

	private void addUserInfo(TarHeader th) {
		th.userId = userData.getUId();
		th.groupId = userData.getGId();
		th.userName = new StringBuffer(userData.getUserName());
		th.groupName = new StringBuffer(userData.getGroupName());
	}

	@Override
	public void putPhysicalFile(String name, Path path) throws IOException {
		TarEntry entry = new TarEntry(path.toFile(), name);
		addUserInfo(entry.getHeader());
		tos.putNextEntry(entry);

		try (InputStream fis = Files.newInputStream(path)) {
			byte[] buffer = new byte[2048];
			int readLen;
			while ((readLen = fis.read(buffer)) > 0) {
				tos.write(buffer, 0, readLen);
			}
		}
	}

	@Override
	public void putFile(String name, long size, InputStream is, boolean executable) throws IOException {
		TarHeader header = TarHeader.createHeader(name, size, System.currentTimeMillis() / 1000, false, executable ? 0x755 : 0x644);
		TarEntry entry = new TarEntry(header);
		tos.putNextEntry(entry);

		byte[] buffer = new byte[2048];
		long remaining = size;
		while (remaining > 0) {
			int readLen = is.read(buffer, 0, (int) Math.min(size, buffer.length));
			if (readLen < 0) {
				throw new EOFException();
			}

			tos.write(buffer, 0, readLen);

			remaining -= readLen;

		}
	}

	@Override
	public void close() throws IOException {
		tos.close();
	}

}
