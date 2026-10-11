// Copyright 2026, Christopher Banes and the Haze project contributors
// SPDX-License-Identifier: Apache-2.0

pluginManagement {
  repositories {
    maven("https://central.sonatype.com/repository/maven-snapshots/") {
      mavenContent { snapshotsOnly() }
      content { includeGroup("io.github.compose-fluent") }
    }
    gradlePluginPortal()
    mavenCentral()
    google()
  }
  resolutionStrategy.eachPlugin {
    if (requested.id.id == "compose-winui") {
      useModule("io.github.compose-fluent:compose-winui:${requested.version}")
    }
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    google()
    maven("https://central.sonatype.com/repository/maven-snapshots/") {
      mavenContent { snapshotsOnly() }
      content { includeGroup("io.github.compose-fluent.compose.components") }
    }
  }
}

rootProject.name = "haze-winui"
include(":haze", ":haze-blur", ":haze-utils")

listOf("haze", "haze-blur", "haze-utils").forEach { module ->
  project(":$module").projectDir = file("../$module")
  project(":$module").buildFileName = "build-winui.gradle.kts"
}
