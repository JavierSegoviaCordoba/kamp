plugins {
  `kotlin-dsl`
}

repositories {
  mavenLocal()
  jcenter()
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation("com.javiersc.gradle-plugins:nexus:0.1.0-alpha.24")
  implementation("com.javiersc.gradle-plugins:publish-kotlin-jvm:0.1.0-alpha.24")
  implementation("com.javiersc.gradle-plugins:publish-kotlin-multiplatform:0.1.0-alpha.24")
}
