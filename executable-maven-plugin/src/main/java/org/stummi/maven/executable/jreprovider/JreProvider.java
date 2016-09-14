package org.stummi.maven.executable.jreprovider;

import java.io.IOException;

import org.apache.maven.project.MavenProject;
import org.stummi.maven.executable.archiveBuilder.ArchiveBuilder;

/**
 * Interface for JRE providing strategies
 */
public interface JreProvider {
	/**
	 * Adds the JRE provided by this implementation to the archive
	 */
	void addToArchive(MavenProject project, ArchiveBuilder builder) throws IOException;
}
