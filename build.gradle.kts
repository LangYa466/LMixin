plugins {
    kotlin("jvm") version "2.1.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "cn.langya"
version = "1.01-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.8")
    implementation("org.ow2.asm:asm-commons:9.8")
    implementation("org.ow2.asm:asm-util:9.8")
    implementation("org.ow2.asm:asm-tree:9.8")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.1")
}

kotlin {
    jvmToolchain(8)
}

val runtimeWithoutKtlib = configurations.create("runtimeWithoutKtlib") {
    extendsFrom(configurations.runtimeClasspath.get())
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-test")
    exclude(group = "org.junit.jupiter", module = "junit-jupiter-api")
    exclude(group = "org.junit.jupiter", module = "junit-jupiter-engine")
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveClassifier.set("with-ktlib")
        mergeServiceFiles()
    }

    register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJarWithoutKtlib") {
        archiveClassifier.set("without-ktlib")
        from(sourceSets.main.get().output)
        configurations = listOf(runtimeWithoutKtlib)
        mergeServiceFiles()
    }

    register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJarWithoutLibs") {
        archiveClassifier.set("without-libs")
        from(sourceSets.main.get().output)
        mergeServiceFiles()
    }

    named<Jar>("jar") {
        enabled = false
    }

    named("build") {
        dependsOn("shadowJar", "shadowJarWithoutKtlib", "shadowJarWithoutLibs")
    }
}