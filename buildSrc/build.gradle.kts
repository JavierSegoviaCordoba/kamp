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
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.30")
  implementation("com.javiersc.gradle-plugins:nexus:0.1.0-alpha.24")
  implementation("com.javiersc.gradle-plugins:publish-kotlin-jvm:0.1.0-alpha.24")
  implementation("com.javiersc.gradle-plugins:publish-kotlin-multiplatform:0.1.0-alpha.24")
}
