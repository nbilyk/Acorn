/*
 * Copyright 2019 Poly Forest, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("UnstableApiUsage")

import java.util.Properties
import org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension
import org.gradle.kotlin.dsl.java as javax

plugins {
    idea
    `maven-publish`
    `java-gradle-plugin`
    kotlin("jvm")
}

buildscript {
    val props = java.util.Properties()
    props.load(projectDir.resolve("../gradle.properties").inputStream())
    val kotlinVersion: String by props
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-sam-with-receiver:$kotlinVersion")
    }
}
apply(plugin = "kotlin-sam-with-receiver")

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

fun Project.samWithReceiver(configure: SamWithReceiverExtension.() -> Unit): Unit = extensions.configure("samWithReceiver", configure)

val props = Properties()
props.load(projectDir.resolve("../gradle.properties").inputStream())
version = props["version"]!!
group = props["group"]!!

repositories {
    jcenter()
    gradlePluginPortal()
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlin-dev/")
    }
}

val kotlinVersion: String by props
val dokkaVersion: String by props

dependencies {
    compileOnly(gradleKotlinDsl())
    compileOnly(gradleApi())
    implementation(kotlin("gradle-plugin", version = kotlinVersion))
    implementation(kotlin("gradle-plugin-api", version = kotlinVersion))
    implementation(kotlin("serialization", version = kotlinVersion))
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")

    testImplementation(gradleKotlinDsl())
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test", version = kotlinVersion))
    testImplementation(kotlin("test-junit", version = kotlinVersion))
}

kotlin {
    sourceSets.all {
        languageSettings.useExperimentalAnnotation("kotlin.Experimental")
    }
    target {
        compilations.all {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_1_8.toString()
            }
        }
    }
}

javax {
//    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

publishing {
    repositories {
        maven {
            url = uri(project.projectDir.resolve("../build/artifacts"))
        }
    }
}

gradlePlugin {
    plugins {
        create("kotlinMpp") {
            id = "com.acornui.kotlin-mpp"
            implementationClass = "com.acornui.build.plugins.KotlinMppPlugin"
            displayName = "Kotlin multi-platform configuration for Acorn UI"
            description = "Configures an Acorn UI library project for Kotlin multi-platform."
        }
        create("kotlinJvm") {
            id = "com.acornui.kotlin-jvm"
            implementationClass = "com.acornui.build.plugins.KotlinJvmPlugin"
            displayName = "Kotlin jvm configuration for Acorn UI"
            description = "Configures an Acorn UI library project for Kotlin jvm."
        }
        create("kotlinJs") {
            id = "com.acornui.kotlin-js"
            implementationClass = "com.acornui.build.plugins.KotlinJsPlugin"
            displayName = "Kotlin js configuration for Acorn UI"
            description = "Configures an Acorn UI library project for Kotlin js."
        }
    }
}