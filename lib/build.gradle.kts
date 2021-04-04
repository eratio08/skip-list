group = "de.elurz"
version = "1.0-SNAPSHOT"

val javaVersion: String by project

plugins {
    kotlin("jvm")
    `java-library`
    idea
}

repositories {
    jcenter()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        apiVersion = "1.4"
        languageVersion = "1.4"
        jvmTarget = javaVersion
        useIR = true
    }
}