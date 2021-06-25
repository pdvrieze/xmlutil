import net.devrieze.gradle.ext.configureDokka
import net.devrieze.gradle.ext.doPublish
import org.gradle.jvm.tasks.Jar

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

plugins {
    kotlin("jvm")
    id("java-library")
    id("kotlinx-serialization")
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

val xmlutil_serial_version: String by project
val xmlutil_core_version: String by project
val xmlutil_versiondesc: String by project

base {
    archivesBaseName = "ktor"
    version = xmlutil_serial_version
}

java {
    withSourcesJar()
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
            languageVersion = "1.5"
            apiVersion = "1.5"
            useExperimentalAnnotation("kotlin.RequiresOptIn")
        }
    }
}

dependencies {
    api(project(":serialization"))
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-serialization:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
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
