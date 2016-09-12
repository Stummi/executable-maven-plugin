package org.stummi.maven.executable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

import lombok.Value;

@Mojo(name = "build-executable", defaultPhase = LifecyclePhase.PACKAGE)
public class BuildExecutableMojo extends AbstractMojo {
	private static final String PLUGIN_VERSION;

	@Parameter(property = "executable.chmod", defaultValue = "755")
	private String chmod;

	@Parameter(readonly = true, defaultValue = "${project}")
	private MavenProject mavenProject;

	@Parameter(property = "executable.dataoffset", defaultValue = "4096")
	private long dataOffset;

	static {
		try (InputStream in = BuildExecutableMojo.class.getResourceAsStream("/executable-maven-plugin-version")) {
			PLUGIN_VERSION = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).readLine();
		} catch (IOException e) {
			throw new RuntimeException(e.getLocalizedMessage(), e);
		}
	}

	private static final List<PosixPermissionFlag> POSIX_FLAGS = Arrays.asList( //
			new PosixPermissionFlag(0001, PosixFilePermission.OTHERS_EXECUTE), //
			new PosixPermissionFlag(0002, PosixFilePermission.OTHERS_WRITE), //
			new PosixPermissionFlag(0004, PosixFilePermission.OTHERS_READ), //

			new PosixPermissionFlag(0010, PosixFilePermission.GROUP_EXECUTE), //
			new PosixPermissionFlag(0020, PosixFilePermission.GROUP_WRITE), //
			new PosixPermissionFlag(0040, PosixFilePermission.GROUP_READ), //

			new PosixPermissionFlag(0100, PosixFilePermission.OWNER_EXECUTE), //
			new PosixPermissionFlag(0200, PosixFilePermission.OWNER_WRITE), //
			new PosixPermissionFlag(0400, PosixFilePermission.OWNER_READ) //
	);

	@Value
	private static class PosixPermissionFlag {
		int flag;
		PosixFilePermission perm;
	}

	@Override
	public void execute() throws MojoExecutionException {
		Build build = mavenProject.getBuild();
		String projectName = build.getFinalName();
		Path buildPath = Paths.get(build.getDirectory());
		Path sourcePath = buildPath.resolve(projectName + ".jar");
		Path targetPath = buildPath.resolve(projectName);

		getLog().info("creating executable file: " + targetPath);
		try (FileOutputStream fos = new FileOutputStream(targetPath.toFile());
				OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
			Files.setPosixFilePermissions(targetPath, parseChmodString(chmod));
			writeWrapperScript(writer);
			setDataOffsetPosition(fos.getChannel());
			appendArchiveData(fos, sourcePath);
		} catch (IOException e) {
			throw new MojoExecutionException("Could not create tar file", e);
		}

	}

	private void appendArchiveData(FileOutputStream fos, Path jarFile) throws IOException {
		try (GZIPOutputStream gzos = new GZIPOutputStream(fos); TarArchiveBuilder tab = new TarArchiveBuilder(gzos)) {
			File javaHome = findJavaHome();
			tab.addDirectoryRecursive("jre", javaHome);
			tab.putPhysicalFile(jarFile.getFileName().toString(), jarFile.toFile());
		}
	}

	private File findJavaHome() throws IOException {
		File javaHome = new File(System.getProperty("java.home"));
		File jreHome = new File(javaHome, "jre");
		File ret = jreHome.exists() ? jreHome : javaHome;

		if (!Files.isExecutable(new File(ret, "bin/java").toPath())) {
			throw new IOException("java.home seems to not point to a valid jre");
		}
		return ret;
	}

	private void setDataOffsetPosition(SeekableByteChannel channel) throws IOException {
		long position = channel.position();
		if (position >= dataOffset) {
			getLog().error("Wrapper script is larger then the data offset (" + position + " >= " + dataOffset
					+ "\nIncrease the dataOffset value in plugin configuration");
			throw new IOException("script length exceeded data offset");
		} else {
			getLog().info("wrapper script size: " + position);
		}

		channel.position(dataOffset);

	}

	private void writeWrapperScript(OutputStreamWriter writer) throws IOException {
		Handlebars hb = new Handlebars(new ClassPathTemplateLoader("/org/stummi/maven/executable/"));
		Template tpl = hb.compile("wrapper");
		Map<String, Object> ctx = new HashMap<>();
		ctx.put("project", mavenProject);
		ctx.put("dataOffset", dataOffset);
		ctx.put("pluginVersion", PLUGIN_VERSION);
		ctx.put("buildTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new Date()));
		tpl.apply(ctx, writer);
		writer.flush();
	}

	private Set<PosixFilePermission> parseChmodString(String str) {
		int chmod = Integer.parseInt(str, 8);
		return POSIX_FLAGS.stream().filter(p -> (p.flag & chmod) != 0).map(p -> p.perm).collect(Collectors.toSet());

	}

}