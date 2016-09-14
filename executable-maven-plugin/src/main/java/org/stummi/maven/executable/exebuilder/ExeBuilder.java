package org.stummi.maven.executable.exebuilder;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.maven.project.MavenProject;
import org.stummi.maven.executable.jreprovider.JreProvider;

/**
 * Interface for classes creating executable files from a java project
 */
public interface ExeBuilder {
	/**
	 * converts a application name to a typical filename for the platform (for
	 * example by appending the .exe suffix on Windows)
	 */
	String toPlatformSpecificBinaryName(String basename);

	/**
	 * creates the executable for the given maven project
	 */
	void createExe(MavenProject mavenProject, Path source, Path out, JreProvider provider, String executableJarFilename) throws IOException;
}
