# Raft protocol implementation in Java

* [Implement RAFT protocol](docs/raft.pdf) (leader election)
* Written in `Java 23`. Version specified inside `.sdkmanrc` file using [sdkman](https://sdkman.io/usage)
* Maven `v3.9.9` with the [wrapper](https://maven.apache.org/wrapper/)

[//]: # (* Compiled to native executable using [GraalVM]&#40;https://www.graalvm.org/&#41;)

* Uses [virtual threads](https://docs.oracle.com/en/java/javase/23/core/virtual-threads.html)
  and [structured concurrency](https://docs.oracle.com/en/java/javase/23/core/structured-concurrency.html)
* Uses [Error Prone](https://errorprone.info/) as an additional compiler to `javac`.
* Uses [Spotless](https://github.com/diffplug/spotless/) for automatic code formatting
  in [Android Open Source Project](https://source.android.com/docs/setup/contribute/code-style) style.

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
./run1.sh

./run2.sh

./run3.sh
```

### Native image <-- NOT WORKING, requires logback replacement for native build

[//]: # (* Build native image using maven `native` profile)

[//]: # ()

[//]: # (If you're using Windows make sure you have [Visual Studio 2022]&#40;https://visualstudio.microsoft.com/downloads/&#41;)

[//]: # (installed.)

[//]: # (It's necessary for the native image compilation.)

[//]: # ()

[//]: # (```bash)

[//]: # (./mvnw clean package -Pnative)

[//]: # (```)

[//]: # ()

[//]: # (* Run native executable &#40;Windows or Unix&#41;)

[//]: # ()

[//]: # (```bash)

[//]: # (./target/jraft.exe)

[//]: # ()

[//]: # (./target/jraft)

[//]: # (```)
