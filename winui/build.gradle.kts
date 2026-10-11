// Copyright 2026, Christopher Banes and the Haze project contributors
// SPDX-License-Identifier: Apache-2.0

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.jetbrains.compose.winui.ComposeWinUiExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
  kotlin("multiplatform") version "2.4.20" apply false
  kotlin("plugin.compose") version "2.4.20" apply false
  id("org.jetbrains.compose") version "1.12.1" apply false
  id("compose-winui") version "1.12-SNAPSHOT" apply false
  id("dev.drewhamilton.poko") version "0.23.2" apply false
  id("com.diffplug.spotless") version "8.10.2"
}

subprojects {
  apply(plugin = "org.jetbrains.kotlin.multiplatform")
  apply(plugin = "org.jetbrains.kotlin.plugin.compose")
  apply(plugin = "org.jetbrains.compose")
  apply(plugin = "compose-winui")
  apply(plugin = "maven-publish")
  apply(plugin = "com.diffplug.spotless")
  layout.buildDirectory.set(rootProject.layout.buildDirectory.dir(name))
  if (name != "haze-utils") {
    apply(plugin = "dev.drewhamilton.poko")
    extensions.configure<dev.drewhamilton.poko.gradle.PokoPluginExtension> {
      pokoAnnotation.set("dev/chrisbanes/haze/Poko")
    }
  }

  group = "io.github.compose-fluent.haze"
  version = providers.gradleProperty("haze.winuiVersion").getOrElse("2.0.1-winui-SNAPSHOT")
  extensions.configure<ComposeWinUiExtension> { jvm.set(true) }
  extensions.configure<KotlinMultiplatformExtension> {
    jvmToolchain(25)
    jvm { compilerOptions.jvmTarget.set(JvmTarget.JVM_25) }
    mingwX64("mingw")
    explicitApi()
    compilerOptions {
      if (project.name != "haze-utils") {
        optIn.add("dev.chrisbanes.haze.ExperimentalHazeApi")
      }
      optIn.add("dev.chrisbanes.haze.InternalHazeApi")
      freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    sourceSets {
      val moduleName = project.name
      val commonMain = getByName("commonMain") {
        dependencies {
          api("io.github.compose-fluent.compose.ui:ui:1.12.0-alpha01")
          api("io.github.compose-fluent.compose.ui:ui-geometry:1.12.0-alpha01")
          api("io.github.compose-fluent.compose.ui:ui-unit:1.12.0-alpha01")
          when (moduleName) {
            "haze" -> {
              api("io.github.compose-fluent.compose.animation:animation-core:1.12.0-alpha01")
              implementation(project(":haze-utils"))
              implementation("androidx.collection:collection:1.6.0")
              // The WinUI coordinate forwards to this published API on both Windows targets.
              implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
            }
            "haze-blur" -> {
              api(project(":haze"))
              implementation(project(":haze-utils"))
              implementation("io.github.compose-fluent.compose.animation:animation-core:1.12.0-alpha01")
              implementation("androidx.collection:collection:1.6.0")
            }
          }
        }
      }
      val skikoMain = create("skikoMain") {
        dependsOn(commonMain)
      }
      getByName("jvmMain") {
        dependsOn(skikoMain)
      }
      getByName("mingwMain") {
        dependsOn(skikoMain)
      }
      getByName("commonTest") {
        kotlin.setSrcDirs(emptyList<String>())
        if (moduleName == "haze") {
          kotlin.srcDir("src/commonTest/kotlin")
          kotlin.include("**/LifecycleTrimMemoryTestPlatform.kt")
        }
        dependencies {
          implementation(kotlin("test"))
          implementation("com.willowtreeapps.assertk:assertk:0.28.1")
        }
      }
      getByName("jvmTest") {
        kotlin.setSrcDirs(emptyList<String>())
        if (moduleName == "haze") {
          kotlin.srcDir("src/jvmTest/kotlin")
          kotlin.include("**/LifecycleTrimMemoryTestPlatform.jvm.kt")
        }
      }
    }
  }

  extensions.configure<PublishingExtension> {
    repositories.maven {
      name = "WinuiExperiment"
      url = uri(
        providers.gradleProperty("haze.winuiRepository").getOrElse(
          rootProject.layout.buildDirectory.dir("winui-maven").get().asFile.absolutePath,
        ),
      )
    }
    publications.withType<MavenPublication>().configureEach {
      pom {
        name.set("Haze WinUI ${project.name}")
        description.set("Haze Skia effects for Compose WinUI JVM and MinGW")
        url.set("https://github.com/compose-fluent/haze")
        licenses {
          license {
            name.set("The Apache License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }
        scm {
          url.set("https://github.com/compose-fluent/haze")
          connection.set("scm:git:https://github.com/compose-fluent/haze.git")
        }
      }
    }
  }
  extensions.configure<SpotlessExtension> {
    kotlin {
      target("src/mingwMain/**/*.kt", "src/mingwTest/**/*.kt")
      ktlint("1.0.1")
    }
    kotlinGradle {
      target("build-winui.gradle.kts")
      ktlint("1.0.1")
    }
  }
  tasks.withType<Jar>().configureEach {
    if (name.endsWith("sourcesJar", ignoreCase = true)) {
      duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
  }
  tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
  }
}

extensions.configure<SpotlessExtension> {
  kotlinGradle {
    target("*.gradle.kts")
    ktlint("1.0.1")
  }
}
