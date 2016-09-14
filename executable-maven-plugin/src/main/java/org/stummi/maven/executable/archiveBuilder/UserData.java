package org.stummi.maven.executable.archiveBuilder;

import org.apache.maven.plugins.annotations.Parameter;

import lombok.Data;

@Data
public class UserData {
	@Parameter
	private int uId = 1000;
	@Parameter
	private int gId = 1000;

	@Parameter
	private String userName = "";

	@Parameter
	private String groupName = "";

}
