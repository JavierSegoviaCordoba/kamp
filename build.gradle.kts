plugins {
  `javiersc-nexus`
}

allprojects {
  group = "lt.petuska"
  version = "0.1.0"
  apply(plugin = "idea")
  repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://kotlin.bintray.com/kotlinx")
  }
  tasks {
    withType<Test> { useJUnitPlatform() }
  }
}
