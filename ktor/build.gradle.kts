/*
 * Copyright (c) 2021.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import net.devrieze.gradle.ext.configureDokka
import net.devrieze.gradle.ext.doPublish
import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm")
    id("java-library")
    alias(libs.plugins.kotlinSerialization)
    `maven-publish`
    signing
    id("org.jetbrains.dokka")
    alias(libs.plugins.binaryValidator)
}

val ktor_version: String by project
val kotlin_version: String get() = libs.versions.kotlin.get()

val xmlutil_serial_version: String by project
val xmlutil_core_version: String by project
val xmlutil_versiondesc: String by project

base {
    archivesName.set("ktor")
    version = xmlutil_serial_version
}

java {
    withSourcesJar()
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

kotlin {
    explicitApi()

    target {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    sourceSets.all {
        languageSettings.apply {
            languageVersion = "1.6"
            apiVersion = "1.6"
            optIn("kotlin.RequiresOptIn")
        }
    }
}

dependencies {
    api(project(":serialization"))
    implementation(libs.ktor.serialization)
    testImplementation(libs.ktor.server.core)
    testImplementation(libs.ktor.server.netty)
    testImplementation(libs.logback.classic)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(kotlin("test"))
}

publishing {
    publications {
        create<MavenPublication>("ktor") {
            from(components["java"])
        }
    }
}

doPublish()

configureDokka(myModuleVersion = xmlutil_core_version)
