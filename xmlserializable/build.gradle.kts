/*
 * Copyright (c) 2023.
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

import net.devrieze.gradle.ext.applyDefaultXmlUtilHierarchyTemplate
import net.devrieze.gradle.ext.configureDokka
import net.devrieze.gradle.ext.doPublish

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlinSerialization)
    `maven-publish`
    signing
    id("org.jetbrains.dokka")
    idea
    alias(libs.plugins.binaryValidator)
}

val xmlutil_version: String by project
val xmlutil_versiondesc: String by project

base {
    archivesName.set("xmlserializable")
    version = xmlutil_version
}

val serializationVersion: String get() = libs.versions.kotlinx.serialization.get()

val autoModuleName = "net.devrieze.xmlutil.xmlserializable"

val testTask = tasks.create("test") {
    group = "verification"
}
val cleanTestTask = tasks.create("cleanTest") {
    group = "verification"
}

kotlin {
    explicitApi()
    applyDefaultXmlUtilHierarchyTemplate()

    jvm {
        compilations.all {
            tasks.named<Test>("${target.name}Test") {
                testTask.dependsOn(this)
            }
            cleanTestTask.dependsOn(tasks.getByName("clean${target.name[0].toUpperCase()}${target.name.substring(1)}Test"))
            tasks.named<Jar>("jvmJar") {
                manifest {
                    attributes("Automatic-Module-Name" to autoModuleName)
                }
            }
        }
    }
    jvm("android") {
        compilations.all {
            tasks.named<Test>("${target.name}Test") {
                testTask.dependsOn(this)
            }
            cleanTestTask.dependsOn(tasks.named("clean${target.name[0].toUpperCase()}${target.name.substring(1)}Test"))
        }
    }
    js {
        browser()
        compilations.all {
            kotlinOptions {
                sourceMap = true
                sourceMapEmbedSources = "always"
                suppressWarnings = false
                verbose = true
                metaInfo = true
                moduleKind = "umd"
                main = "call"
            }
        }
    }

    targets.all {
        mavenPublication {
            version = xmlutil_version
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core")) // Don't add a runtime dep here
                implementation(libs.serialization.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.junit5.api)

                runtimeOnly(libs.junit5.engine)
                runtimeOnly(libs.woodstox)
            }
        }

        val androidMain by getting {
            dependencies {
                compileOnly(libs.kxml2)
            }
        }

        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.junit5.api)

                runtimeOnly(libs.junit5.engine)
                runtimeOnly(libs.kxml2)
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

    }
    sourceSets.all {
        languageSettings {
            optIn("nl.adaptivity.xmlutil.XmlUtilInternal")
            optIn("nl.adaptivity.xmlutil.ExperimentalXmlUtilApi")
        }
    }

}

doPublish()

configureDokka(myModuleVersion = xmlutil_version)

idea {
    module {
        name = "xmlutil-xmlserializable"
    }
}
