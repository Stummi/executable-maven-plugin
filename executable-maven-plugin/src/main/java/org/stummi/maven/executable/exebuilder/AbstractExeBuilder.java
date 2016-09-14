package org.stummi.maven.executable.exebuilder;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.maven.project.MavenProject;
import org.stummi.maven.executable.archiveBuilder.ArchiveBuilder;
import org.stummi.maven.executable.jreprovider.JreProvider;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Synchronized;

/**
 * Abstract superclass for ExeBuilder implementations. The class has state
 * interally but is thread safe due to synchronization
 */
@Getter(AccessLevel.PROTECTED)
public abstract class AbstractExeBuilder implements ExeBuilder {

	private MavenProject mavenProject;
	private Path source;
	private Path out;
	private JreProvider provider;
	private String executableJar;

	@Override
	@Synchronized
	public void createExe(MavenProject mavenProject, Path source, Path out, JreProvider provider, String executableJarFilename) throws IOException {
		this.mavenProject = mavenProject;
		this.source = source;
		this.out = out;
		this.provider = provider;
		this.executableJar = executableJarFilename;

		try (SeekableByteChannel channel = Files.newByteChannel(out, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING); ArchiveBuilder archiveBuilder = createExecutableBase(channel)) {
			if (Files.isDirectory(source)) {
				archiveBuilder.putPhysicalDirectoryRecursive("", source);
			} else {
				archiveBuilder.putPhysicalFile(source.getFileName().toString(), source);
			}
			provider.addToArchive(mavenProject, archiveBuilder);
		}
		afterExecutableBuilt();
	}

	/**
	 * Postprocess the built executable
	 */
	protected void afterExecutableBuilt() throws IOException {
		// to be overridden from implementations
	}

	/**
	 * prepares the executable file and returns an {@link ArchiveBuilder}
	 * instance which will be used to append the Application and the Jre
	 */
	protected abstract ArchiveBuilder createExecutableBase(SeekableByteChannel channel) throws IOException;
}
