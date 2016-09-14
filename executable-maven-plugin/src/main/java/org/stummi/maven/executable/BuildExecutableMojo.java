package org.stummi.maven.executable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.stummi.maven.executable.exebuilder.PosixExeBuilder;
import org.stummi.maven.executable.jreprovider.OneOfJreProviders;

@Mojo(name = "build-executable", defaultPhase = LifecyclePhase.PACKAGE)
public class BuildExecutableMojo extends AbstractMojo {
	@Parameter(readonly = true, defaultValue = "${project}")
	private MavenProject mavenProject;

	@Parameter(property = "executable.dataoffset", defaultValue = "4096")
	private long dataOffset;

	@Parameter
	private OneOfJreProviders jreProvider = OneOfJreProviders.system();

	@Parameter
	private String target;

	@Parameter
	private String source;

	@Parameter
	private String runnableJarFile;

	@Override
	public void execute() throws MojoExecutionException {
		PosixExeBuilder exeBuilder = new PosixExeBuilder();

		Build build = mavenProject.getBuild();
		String projectName = build.getFinalName();
		Path buildPath = Paths.get(build.getDirectory());

		Path sourcePath = source != null ? Paths.get(source) : buildPath.resolve(projectName + ".jar");
		Path targetPath = target != null ? Paths.get(target) : buildPath.resolve(exeBuilder.toPlatformSpecificBinaryName(projectName));
		String jarFile = runnableJarFile != null ? runnableJarFile : sourcePath.getFileName().toString();

		getLog().info("creating executable file: " + targetPath);

		try {
			exeBuilder.createExe(mavenProject, sourcePath, targetPath, jreProvider.getProvider(), jarFile);
		} catch (IOException e) {
			throw new MojoExecutionException("Could not create exeuctable file", e);
		}

	}

}