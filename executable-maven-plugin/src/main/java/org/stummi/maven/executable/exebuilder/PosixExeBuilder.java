package org.stummi.maven.executable.exebuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.stummi.maven.executable.PluginVersion;
import org.stummi.maven.executable.archiveBuilder.ArchiveBuilder;
import org.stummi.maven.executable.archiveBuilder.TarArchiveBuilder;
import org.stummi.maven.executable.archiveBuilder.UserData;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

import lombok.extern.slf4j.Slf4j;

/**
 * Builds executable which should be runnable on most POSIX compatible systems.
 */
@Slf4j
public class PosixExeBuilder extends AbstractExeBuilder {
	private static final Set<PosixFilePermission> EXECUTABLE_PERMISSION = new HashSet<>(Arrays.asList(PosixFilePermission.GROUP_READ,
			PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE, PosixFilePermission.OWNER_READ,
			PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_WRITE));

	@Override
	public String toPlatformSpecificBinaryName(String basename) {
		return basename;
	}

	@Override
	protected ArchiveBuilder createExecutableBase(SeekableByteChannel channel) throws IOException {
		int dataOffset = 4096;
		OutputStream os = Channels.newOutputStream(channel);
		OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
		writeWrapperScript(dataOffset, writer);
		long position = channel.position();
		log.info("wrapper script size: " + position);
		if (position > dataOffset) {
			// if we exceeded the data offset for some reason, increase it.
			// +1 because the increased data offset may need one more byte in
			// the script
			while ((position + 1) > dataOffset) {
				dataOffset *= 2;
			}
			channel.position(0);
			log.info("increased data offset to " + dataOffset);
			writeWrapperScript(dataOffset, writer);
		}
		channel.position(dataOffset);
		GZIPOutputStream gzos = new GZIPOutputStream(os);
		return new TarArchiveBuilder(gzos, new UserData());
	}

	@Override
	protected void afterExecutableBuilt() throws IOException {
		Files.setPosixFilePermissions(getOut(), EXECUTABLE_PERMISSION);
	}

	private void writeWrapperScript(int dataOffset, OutputStreamWriter writer) throws IOException {
		Handlebars hb = new Handlebars(new ClassPathTemplateLoader("/org/stummi/maven/executable/"));
		Template tpl = hb.compile("wrapper");
		Map<String, Object> ctx = new HashMap<>();
		ctx.put("project", getMavenProject());
		ctx.put("dataOffset", dataOffset);
		ctx.put("pluginVersion", PluginVersion.PLUGIN_VERSION);
		ctx.put("buildTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new Date()));
		ctx.put("jarFile", getExecutableJar());
		tpl.apply(ctx, writer);
		writer.flush();
	}

}
