# executable-maven-plugin

An maven plugin which packages your java application together with a JRE into a single executable archive file. The result is a single file with minimal dependencies on the host system, which can be dropped and run anywhere by a User, similar to statically linked C or C++ applications

This project is just a proof of concept and far from beeing usable productive. The biggest limitation right now is that it only will run on Linux and only create Linux compatible executable files.

## Usage

An example plugin configuration can be found in the [test-executable-maven-plugin](test-executable-maven-plugin/pom.xml) project. To use it, simply type:

```sh
$ mvn package
```

which creates a runnable file under target with the same name as the jar file name (without the .jar extension). You just can invoke this file

```sh
$ target/test-executable-maven-plugin-0.1.0-SNAPSHOT
Hello, World!
```
