plugins {
    kotlin("jvm") version "2.1.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "cn.langya"
version = "1.0-SNAPSHOT"

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

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveClassifier.set("with-ktlib")
        mergeServiceFiles()
        dependencies {
            exclude(dependency("org.junit.jupiter:junit-jupiter-api"))
            exclude(dependency("org.junit.jupiter:junit-jupiter-engine"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-test"))
        }
    }

    register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJarWithoutKtlib") {
        archiveClassifier.set("without-ktlib")
        from(sourceSets.main.get().output)
        configurations = listOf(project.configurations.runtimeClasspath.get())
        mergeServiceFiles()
        dependencies {
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-reflect"))
            exclude(dependency("org.junit.jupiter:junit-jupiter-api"))
            exclude(dependency("org.junit.jupiter:junit-jupiter-engine"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-test"))
        }
    }

    register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJarWithoutLibs") {
        archiveClassifier.set("without-libs")
        from(sourceSets.main.get().output)
        configurations = listOf(project.configurations.runtimeClasspath.get())
        mergeServiceFiles()
    }

    named<Jar>("jar") {
        enabled = false
    }

    named("build") {
        dependsOn("shadowJar", "shadowJarWithoutKtlib", "shadowJarWithoutLibs")
    }
}


