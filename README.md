# FleetCheck - Gradle

FleetCheck is a Java 21 application used to reproduce the same quality-oriented build process previously implemented with Maven, but using Gradle.

The same Java source code, resources and tests are used in both versions. The main purpose is to compare how Maven and Gradle express dependency management, testing, packaging, wrappers, continuous integration and SBOM generation.

---

## Environment

The project was developed and tested with:

- Java 21
- Gradle 9.8.0
- Git
- GitHub
- GitHub Actions

Gradle version verification:

```text
Gradle 9.8.0
Launcher JVM: 21.0.12.1
OS: Windows 11
```

---

# Evidence 8.1 - Initial Gradle build failure

The initial `build.gradle` intentionally did not contain the Jackson application dependency.

The following command was executed:

```bash
gradle clean build
```

The build failed during the `compileJava` task.

Relevant errors:

```text
error: package com.fasterxml.jackson.core.type does not exist
import com.fasterxml.jackson.core.type.TypeReference;
```

```text
error: package com.fasterxml.jackson.databind does not exist
import com.fasterxml.jackson.databind.ObjectMapper;
```

The compiler also reported:

```text
cannot find symbol
symbol: class ObjectMapper
```

and:

```text
cannot find symbol
symbol: class TypeReference
```

The missing application dependency was:

```text
com.fasterxml.jackson.core:jackson-databind:2.22.2
```

It was added to `build.gradle` using:

```gradle
implementation 'com.fasterxml.jackson.core:jackson-databind:2.22.2'
```

Because Gradle 9.8.0 also required the JUnit Platform Launcher explicitly at test runtime, the following dependency was added:

```gradle
testRuntimeOnly 'org.junit.platform:junit-platform-launcher:1.14.4'
```

After adding the required dependencies, the command:

```bash
gradle clean build
```

completed successfully:

```text
BUILD SUCCESSFUL
```

---

# Evidence 8.2 - Dependency graph

The runtime dependency graph was inspected with:

```bash
gradle dependencies --configuration runtimeClasspath
```

Relevant output:

```text
runtimeClasspath - Runtime classpath of source set 'main'.

\--- com.fasterxml.jackson.core:jackson-databind:2.22.2
     +--- com.fasterxml.jackson.core:jackson-annotations:2.22
     +--- com.fasterxml.jackson.core:jackson-core:2.22.2
     |    \--- com.fasterxml.jackson:jackson-bom:2.22.2
     \--- com.fasterxml.jackson:jackson-bom:2.22.2
```

The direct dependency is:

```text
jackson-databind
```

The transitive dependencies include:

```text
jackson-core
jackson-annotations
```

## Did changing the build system change the application dependencies?

No.

Changing from Maven to Gradle did not change the dependencies required by the application.

In both build systems, `jackson-databind` is a direct dependency, while `jackson-core` and `jackson-annotations` are resolved transitively.

The application dependencies remained the same; only the build system and its configuration syntax changed.

---

# Evidence 8.3 - Build and execute the JAR

The default Gradle JAR was first created with:

```bash
gradle clean jar
```

The generated file was:

```text
build/libs/fleetcheck-1.0.0.jar
```

Trying to execute the default JAR with:

```bash
java -jar build\libs\fleetcheck-1.0.0.jar
```

produced:

```text
no main manifest attribute, in build\libs\fleetcheck-1.0.0.jar
```

This showed that the default Gradle JAR was not yet a self-contained executable application.

The `application` plugin was then added:

```gradle
plugins {
    id 'java'
    id 'application'
}
```

The main class was configured as:

```gradle
application {
    mainClass = 'pt.upt.fleetcheck.App'
}
```

The JAR was also configured to include the runtime dependencies:

```gradle
jar {
    manifest {
        attributes 'Main-Class': 'pt.upt.fleetcheck.App'
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from {
        configurations.runtimeClasspath.collect {
            it.isDirectory() ? it : zipTree(it)
        }
    }
}
```

The JAR was rebuilt using:

```bash
gradle clean jar
```

It was then executed with:

```bash
java -jar build\libs\fleetcheck-1.0.0.jar
```

Application output:

```text
FleetCheck 1.0
Vehicles loaded: 4
Vehicles requiring service: 2
Average mileage: 37000 km
```

## What changed in the JAR after the runtime dependencies were included?

The original Gradle JAR contained the FleetCheck project classes but was not directly executable.

After configuring the `Main-Class` attribute and including the runtime dependencies, the JAR became self-contained.

It now contains both the application and the libraries required at runtime, allowing it to be executed directly using:

```bash
java -jar build\libs\fleetcheck-1.0.0.jar
```

---

# Step 8.4 - Gradle Wrapper

The Gradle Wrapper was generated using:

```bash
gradle wrapper
```

The project then contained:

```text
gradlew
gradlew.bat
gradle/wrapper/
```

The wrapper was tested on Windows using:

```bash
gradlew.bat clean build
```

The build completed successfully:

```text
BUILD SUCCESSFUL
```

## Which hidden environmental assumption did the Gradle Wrapper remove?

The Gradle Wrapper removes the assumption that Gradle is already installed and correctly configured on the developer or CI machine.

Instead, the project contains the information required to download and use the expected Gradle version automatically.

This improves build consistency and reproducibility between different developer machines and CI environments.

---

# Evidence 8.5 - GitHub Actions

A separate GitHub repository was created for the Gradle version:

```text
https://github.com/karst23/FleetCheck-Gradle
```

The GitHub Actions workflow was created in:

```text
.github/workflows/build-gradle.yml
```

The workflow is configured to:

- checkout the repository;
- set up Java 21;
- use the Gradle cache;
- execute the Gradle Wrapper;
- run the complete build;
- upload the generated JAR as a workflow artifact.

The main build command used by the workflow is:

```bash
./gradlew clean build
```

The generated artifact is configured as:

```text
fleetcheck-gradle-build
```

## GitHub Actions execution URL


Successful Gradle GitHub Actions execution:

https://github.com/karst23/FleetCheck-Gradle/actions/runs/37374444383

During the initial attempts, GitHub Actions returned the following infrastructure error before the build could start:

```text
Internal server error.
The job was not acquired by Runner of type hosted even after multiple attempts.
```

This error happened before the project build was executed and was related to GitHub-hosted runner availability.

Once a successful execution is available, its URL should be recorded here as required by Evidence 8.5.

---

# Evidence 8.6 - Gradle SBOM

The CycloneDX Gradle plugin was added to the `plugins` block:

```gradle
id 'org.cyclonedx.bom' version '3.4.1'
```

The SBOM was generated using the Gradle Wrapper:

```bash
gradlew.bat cyclonedxBom
```

The command completed successfully:

```text
BUILD SUCCESSFUL
```

The generated files were:

```text
build/reports/cyclonedx/bom.json
build/reports/cyclonedx/bom.xml
```

The worksheet requires the JSON SBOM:

```text
build/reports/cyclonedx/bom.json
```

The SBOM contains components including:

```text
jackson-databind
jackson-core
jackson-annotations
```

## Why does the Gradle SBOM contain dependencies that were not explicitly typed in build.gradle?

The SBOM represents the complete resolved dependency graph, not only the dependencies explicitly declared by the developer.

`jackson-databind` is declared directly in `build.gradle`.

However, `jackson-core` and `jackson-annotations` are dependencies required by Jackson. Gradle resolves these dependencies automatically as transitive dependencies.

Because the SBOM describes the complete set of software components used by the application, these transitive dependencies are also included.

---

# Evidence 8.7 - Maven and Gradle comparison

| Task | Maven | Gradle |
|---|---|---|
| Build configuration | `pom.xml` | `build.gradle` |
| Clean build | `mvnw.cmd clean verify` | `gradlew.bat clean build` |
| Add dependency | `<dependency>...</dependency>` | `implementation 'group:artifact:version'` |
| Inspect dependencies | `mvn dependency:tree` | `gradle dependencies --configuration runtimeClasspath` |
| Wrapper | `mvnw.cmd` | `gradlew.bat` |
| Build output | `target/` | `build/` |
| JAR location | `target/` | `build/libs/` |
| SBOM | CycloneDX Maven Plugin | CycloneDX Gradle Plugin |

---

# Final Question

## Both Maven and Gradle built exactly the same FleetCheck application. What changed: the software or the build process?

The software did not change.

Both Maven and Gradle build the same FleetCheck application using the same Java source code, resources, tests and application dependencies.

What changed was the build process and the way that process was configured.

Maven describes the build using XML in `pom.xml`, while Gradle uses a Groovy-based DSL in `build.gradle`.

Despite their different configuration styles, both build systems were used to perform the same main tasks:

- resolve direct and transitive dependencies;
- compile the Java application;
- run automated tests;
- create an executable JAR;
- use a build wrapper;
- execute the build in GitHub Actions;
- generate a CycloneDX SBOM.

Therefore, the FleetCheck software remained the same. The build system and the way the build process was expressed changed.

---

# Final Gradle Build Configuration

The final `build.gradle` uses:

- Java 21;
- Jackson Databind 2.22.2;
- JUnit Jupiter 5.14.4;
- JUnit Platform Launcher 1.14.4;
- the Java plugin;
- the Application plugin;
- CycloneDX 3.4.1;
- an executable JAR containing runtime dependencies.

The main application class is:

```text
pt.upt.fleetcheck.App
```

The executable JAR is generated in:

```text
build/libs/fleetcheck-1.0.0.jar
```

The SBOM is generated in:

```text
build/reports/cyclonedx/bom.json
```