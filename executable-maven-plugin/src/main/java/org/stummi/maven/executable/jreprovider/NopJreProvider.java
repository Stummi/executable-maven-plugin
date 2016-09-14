package org.stummi.maven.executable.jreprovider;

import java.io.IOException;

import org.apache.maven.project.MavenProject;
import org.stummi.maven.executable.archiveBuilder.ArchiveBuilder;

public class NopJreProvider implements JreProvider {
	@Override
	public void addToArchive(MavenProject project, ArchiveBuilder builder) throws IOException {
		// do nothing
	}
}
