package org.stummi.maven.executable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PluginVersion {
	public static final String PLUGIN_VERSION;

	static {
		PLUGIN_VERSION = loadPluginVersion();
	}

	@SneakyThrows
	private static String loadPluginVersion() {
		try (InputStream in = BuildExecutableMojo.class.getResourceAsStream("/executable-maven-plugin-version")) {
			return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).readLine();
		}
	}
}
