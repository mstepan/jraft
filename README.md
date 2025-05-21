# Raft protocol implementation in Java

* Written in `Java 23`. Version specified inside `.sdkmanrc` file using [sdkman](https://sdkman.io/usage)
* Maven `v3.9.9` with the [wrapper](https://maven.apache.org/wrapper/)
* Compiled to native executable using [GraalVM](https://www.graalvm.org/)
* Uses [virtual threads](https://docs.oracle.com/en/java/javase/23/core/virtual-threads.html) and [structured concurrency](https://docs.oracle.com/en/java/javase/23/core/structured-concurrency.html)
* Uses [Error Prone](https://errorprone.info/) as an additional compiler to `javac`.
* Uses [Spotless](https://github.com/diffplug/spotless/) for automatic code formatting in [Android Open Source Project](https://source.android.com/docs/setup/contribute/code-style) style.
* Uses [OWASP dependency-check](https://owasp.org/www-project-dependency-check/) to detect CVEs inside dependencies.

## Build & run

### Standard maven

* Build self-executable jar file

```bash
./mvnw clean package
```

* Run application
  Pay attention that we also need to provide `--enable-preview` during runtime because we have used
  [Structured Concurrency](https://docs.oracle.com/en/java/javase/23/core/structured-concurrency.html) which is in a
  preview mode for java 23.

```bash
java --enable-preview -jar target/jraft-0.0.1-SNAPSHOT.jar 
```

### Native image

* Build native image using maven `native` profile

If you're using Windows make sure you have [Visual Studio 2022](https://visualstudio.microsoft.com/downloads/)
installed.
It's necessary for the native image compilation.

```bash
./mvnw clean package -Pnative
```

* Run native executable (Windows or Unix)

```bash
./target/jraft.exe

./target/jraft
```

## Quality checks

### OWASP check dependencies for vulnerabilities

* Run OWASP dependency checker. The OWASP checker attached to `check` maven phase that can be triggered using below
  command:

```bash
./mvnw verify
```