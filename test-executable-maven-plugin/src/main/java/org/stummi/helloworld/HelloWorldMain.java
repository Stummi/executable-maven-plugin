package org.stummi.helloworld;

import java.util.stream.Stream;

public class HelloWorldMain {
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Hello, World!");
		} else {
			Stream.of(args).forEach(s -> System.out.println("Hello, " + s + "!"));
		}
	}
}
