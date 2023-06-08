plugins {
    id("java")
    id("application")

    id("com.diffplug.spotless") version "6.18.0"
}

group = "de.nnev"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("de.nnev.mgmt.Manager")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

spotless {
    java {
        googleJavaFormat()
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("info.picocli:picocli:4.7.3")
    annotationProcessor("info.picocli:picocli-codegen:4.7.3")

    implementation("com.unboundid:unboundid-ldapsdk:6.0.8")
    implementation("com.kohlschutter.junixsocket:junixsocket-core:2.6.2")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.withType<JavaCompile> {
    val compilerArgs = options.compilerArgs
    compilerArgs.add("-Aproject=${project.group}/${project.name}")
}

tasks.test {
    useJUnitPlatform()
}
