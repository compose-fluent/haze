# Haze for Compose WinUI

This experimental build publishes Haze 2.0.1 core, Blur, and shared utilities for Windows
JVM and MinGW x64. It reuses the upstream `skikoMain` renderer, including Gaussian blur,
progressive blur, tint, noise, and runtime shaders. Windows source sets provide the clock,
Unicode debugger logging, window identity, and lifecycle-driven resource cleanup.

The build lives in `winui` so the upstream Android, Apple, JVM, and Web build remains available.
Glass and Material style modules are outside this Windows profile.

Use Java 21 to run Gradle and install a Java 25 toolchain for the WinUI JVM artifacts.
The profile uses Kotlin/Compose compiler 2.4.20, Compose Gradle plugin 1.12.1, the published
`compose-winui:1.12-SNAPSHOT` plugin, CMP WinUI 1.12.0-alpha01, and Skiko WinUI 0.150.1-alpha02.
The fork group is `io.github.compose-fluent.haze`; the default version is `2.0.1-winui-SNAPSHOT`.

```powershell
.\gradlew.bat -p winui :haze-utils:mingwTest :haze:mingwTest spotlessCheck --no-scan
.\gradlew.bat -p winui publishAllPublicationsToWinuiExperimentRepository --no-scan
```

The default local Maven repository is `winui/build/winui-maven`. Set
`-Phaze.winuiRepository=E:/path/to/maven` to choose another directory and
`-Phaze.winuiVersion=2.0.1-winui-SNAPSHOT` to set the artifact version explicitly.
Each module has a root metadata artifact, a `-jvm` artifact, and a `-mingw` artifact.
Consumers should declare the root coordinates.

For source builds, include the profile from a pinned checkout or Git submodule:

```kotlin
// settings.gradle.kts
includeBuild("external/haze/winui")
```

For Maven builds, add the local publication directory to the dependency repositories:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
  repositories {
    maven("E:/path/to/maven") {
      content { includeGroup("io.github.compose-fluent.haze") }
    }
    mavenCentral()
    google()
  }
}
```

With the published `compose-winui` plugin configured in the consumer, keep the upstream
Haze dependencies and redirect the Haze version family for enabled Windows targets:

```kotlin
composeWinUi {
  redirectGroup(
    "dev.chrisbanes.haze",
    "2.0.1",
    "2.0.1-winui-SNAPSHOT",
    "io.github.compose-fluent.haze",
  )
}

kotlin {
  mingwX64()
  sourceSets.commonMain.dependencies {
    implementation("dev.chrisbanes.haze:haze:2.0.1")
    implementation("dev.chrisbanes.haze:haze-blur:2.0.1")
  }
}
```

MinGW redirection is enabled by default. JVM redirection requires `jvm.set(true)` on Windows;
other consumer targets retain their existing dependencies. The plugin stages the Skiko DLL
and ICU data beside linked MinGW executables and tests.

The Windows profile declares WinUI root UI coordinates directly so its shared metadata is
visible to both Windows targets. It also declares the published Google Lifecycle Compose
2.11.0 API that the WinUI coordinate forwards to, ensuring `LocalLifecycleOwner` is visible
to metadata compilation. Runtime and lifecycle behavior use the same published implementation.
