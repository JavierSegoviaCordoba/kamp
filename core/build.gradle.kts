import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("multiplatform")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("org.jlleitschuh.gradle.ktlint")
  idea
  `javiersc-publish-kotlin-multiplatform`
}

group = "com.javiersc.kamp"

idea {
  module {
    isDownloadSources = true
    isDownloadJavadoc = true
  }
}

kotlin {
  explicitApi()
  jvm()
  js { browser() }

  sourceSets {
    named("commonMain") { dependencies { api("io.ktor:ktor-client-serialization:_") } }
    named("jvmMain") { dependencies { api(kotlin("reflect")) } }
  }
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "15"
    kotlinOptions {
      useIR = true
      jvmTarget = "${JavaVersion.VERSION_11}"
    }
  }
}
