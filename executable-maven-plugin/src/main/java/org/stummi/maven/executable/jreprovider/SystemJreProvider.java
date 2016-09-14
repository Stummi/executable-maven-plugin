package org.stummi.maven.executable.jreprovider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.stummi.maven.executable.archiveBuilder.ArchiveBuilder;

public class SystemJreProvider implements JreProvider {
	@Parameter
	private String javaHome;

	@Override
	public void addToArchive(MavenProject ptoject, ArchiveBuilder builder) throws IOException {
		Path javaPath = Paths.get(javaHome != null ? javaHome : System.getProperty("java.home"));
		Path jrePath = javaPath.resolve("jre");
		Path jreBase = Files.isDirectory(jrePath) ? jrePath : javaPath;

		String binaryName = System.getProperty("os.name").startsWith("Windows") ? "bin/java.exe" : "bin/java";

		if (!Files.isExecutable(jreBase.resolve(binaryName))) {
			throw new IOException("java.home seems to not point to a valid jre");
		}

		builder.putPhysicalDirectoryRecursive("jre", jreBase);
	}
}
