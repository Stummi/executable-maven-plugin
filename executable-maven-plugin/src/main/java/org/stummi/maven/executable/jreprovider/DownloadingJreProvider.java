package org.stummi.maven.executable.jreprovider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarInputStream;
import org.stummi.maven.executable.archiveBuilder.ArchiveBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * JRE Provider implementation which downloads the JRE from a given URL
 * 
 */
@Slf4j
public class DownloadingJreProvider implements JreProvider {
	@Parameter(required = true)
	private String url;

	@Parameter
	private Map<String, String> checksum;

	@Parameter
	private int stripParts;

	@Parameter
	private String archiveRoot;

	@Parameter
	private String downloadDir;

	@Override
	public void addToArchive(MavenProject project, ArchiveBuilder builder) throws IOException {
		Path dlDir = downloadDir != null ? Paths.get(downloadDir) : Paths.get(project.getBuild().getDirectory(), "jre_downloads");
		Files.createDirectories(dlDir);

		String safeName = url.replaceAll("[\\/\\<\\>\\:\\\"\\\\\\|\\?\\*]", "_");
		Path dlDest = dlDir.resolve(safeName);

		if (!Files.exists(dlDest)) {
			download(url, dlDest);
		}

		if (checksum != null) {
			for (Entry<String, String> e : checksum.entrySet()) {
				assertCorrectChecksum(dlDest, e.getKey(), e.getValue());
			}
		} else {
			log.warn("No checksums defined for downloaded archive. Its recommended to validate downloaded files. "
					+ "If you really want this, add add an empty <checksum /> tag to the configuration to get rid of this warning");
		}

		try (InputStream is = Files.newInputStream(dlDest)) {
			appendJreDataFromArchive(is, builder);
		}

	}

	private void appendJreDataFromArchive(InputStream is, ArchiveBuilder builder) throws IOException {
		// TODO - Some type recognizing magic, support other archive formats
		String root = archiveRoot == null ? "" : archiveRoot;

		// modify the archive root string if necessary, so we can assume it does
		// not start
		// with / but ends with it
		if (root.startsWith("/")) {
			root = root.substring(1);
		}

		if (!root.endsWith("/")) {
			root += "/";
		}

		try (TarInputStream tis = new TarInputStream(new GZIPInputStream(is))) {
			TarEntry entry;
			while ((entry = tis.getNextEntry()) != null) {
				String name = entry.getName();
				String origName = name;

				if (stripParts > 0) {
					int offset = 0;
					for (int idx = 0; idx < stripParts; ++idx) {
						offset = name.indexOf('/', offset) + 1;
						if (offset == -1) {
							break;
						}
					}
					if (offset == -1) {
						continue;
					}
					name = name.substring(offset);
				}

				if (!name.startsWith(root) || name.equals(root)) {
					continue;
				}

				name = name.substring(root.length());

				log.debug("put entry: " + origName + " as " + name);
				if (entry.isDirectory()) {
					builder.putDirectory("jre/" + name);
				} else {
					builder.putFile("jre/" + name, entry.getSize(), tis, (entry.getHeader().mode & 0x111) > 0);
				}
			}
		}
	}

	private static void assertCorrectChecksum(Path dlDest, String algorithm, String sum) throws IOException {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(algorithm.toUpperCase());
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("unknown checksum algorhithm: " + algorithm, e);
		}
		try (InputStream in = Files.newInputStream(dlDest)) {
			byte[] buffer = new byte[1024];
			int readLen;
			while ((readLen = in.read(buffer)) > 0) {
				digest.update(buffer, 0, readLen);
			}
		}

		byte[] data = digest.digest();
		String calculatedSum = createHashSum(data);
		if (!calculatedSum.equalsIgnoreCase(sum)) {
			throw new IOException(algorithm + " hashsum of downloaded file " + (calculatedSum) + " does not equals expected (" + sum + ")");
		}
	}

	private static String createHashSum(byte[] data) {
		BigInteger bigInt = new BigInteger(1, data);
		String hash = bigInt.toString(16);
		StringBuilder sb = new StringBuilder();
		for (int idx = 0; idx < (data.length * 2 - hash.length()); ++idx) {
			sb.append("0");
		}
		return sb.append(hash).toString();
	}

	private static void download(String url, Path dlDest) throws IOException {
		log.info("download: " + url + " to " + dlDest + "...");
		try (InputStream in = new URL(url).openConnection().getInputStream(); OutputStream out = Files.newOutputStream(dlDest)) {
			byte[] buffer = new byte[1024];
			int readLen;
			while ((readLen = in.read(buffer)) > 0) {
				out.write(buffer, 0, readLen);
			}
		}
	}
}
