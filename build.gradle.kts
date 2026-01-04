import org.gradle.jvm.tasks.Jar

plugins {
    id("java")
    id("application")
    id("org.graalvm.buildtools.native") version "0.9.28"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.0")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("org.example.Main")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf("Main-Class" to "org.example.Main"))
    }
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("downloader")
            buildArgs.add("--no-fallback")
            buildArgs.add("--enable-url-protocols=http,https")
            buildArgs.add("--enable-all-security-services")
        }
    }
}
