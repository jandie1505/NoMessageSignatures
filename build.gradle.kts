plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
}

group = "net.jandie1505"
version = "1.3-1.21.10-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

dependencies {
    paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}
