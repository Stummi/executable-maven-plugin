package org.stummi.maven.executable.jreprovider;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Helper class to make serval JRE provider strategies selectable with their own
 * configuration options.
 */
public class OneOfJreProviders {
	@Parameter
	private SystemJreProvider system;

	@Parameter
	private DownloadingJreProvider download;

	@Parameter
	private NopJreProvider none;

	private static final List<Function<OneOfJreProviders, JreProvider>> ACCESSORS = Arrays.asList( //
			p -> p.system, //
			p -> p.download, //
			p -> p.none //
	);

	public JreProvider getProvider() {
		Set<JreProvider> configuredProviders = ACCESSORS.stream().map(f -> f.apply(this)).filter(t -> t != null).collect(Collectors.toSet());
		if (configuredProviders.isEmpty()) {
			throw new IllegalArgumentException("no JRE provider configured.");
		} else if (configuredProviders.size() > 1) {
			throw new IllegalArgumentException("Multiple JRE providers configured");
		} else {
			return configuredProviders.iterator().next();
		}
	}

	public static OneOfJreProviders system() {
		OneOfJreProviders ret = new OneOfJreProviders();
		ret.system = new SystemJreProvider();
		return ret;
	}
}
