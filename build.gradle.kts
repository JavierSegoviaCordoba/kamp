plugins {
  `javiersc-nexus`
}

allprojects {
  group = "lt.petuska"
  version = "0.2.0"
  apply(plugin = "idea")
  repositories {
    mavenCentral()
    maven("https://jitpack.io")
  }
  tasks {
    withType<Test> { useJUnitPlatform() }
  }
}
