/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

@file:Suppress("PropertyName")

import net.devrieze.gradle.ext.configureDokka
import net.devrieze.gradle.ext.doPublish
import net.devrieze.gradle.ext.envJvm
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE

plugins {
    kotlin("multiplatform")
    id("kotlinx-serialization")
    id("maven-publish")
    id("signing")
    idea
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

val xmlutil_serial_version: String by project
val xmlutil_core_version: String by project
val xmlutil_versiondesc: String by project
val xmlutil_util_version: String by project

base {
    archivesName.set("ktor")
    version = xmlutil_serial_version
}

val serializationVersion: String by project

kotlin {
    targets {
        jvm {
            attributes {
                attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, envJvm)
            }
            compilations.all {
                compileKotlinTaskProvider.configure {
                    kotlinOptions {
                        jvmTarget = "1.8"
                    }
                }
            }
            testRuns.all {
                executionTask.configure {
                    useJUnitPlatform()
                }
            }
        }
/*
        jvm("android") {
            attributes {
                attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, envAndroid)
                attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
            }
            compilations.all {
                kotlinOptions {
                    jvmTarget = if (name=="test") "1.8" else "1.6"
                }
            }
        }
*/
        js(BOTH) {
            browser()
            nodejs()
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

    }
    targets.all {
        mavenPublication {
            version = xmlutil_serial_version
        }
    }

    sourceSets {
        val commonMain by getting {
            this.dependencies {
                api(project(":serialization"))
//                implementation("io.ktor:ktor-serialization:$ktor_version")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
                implementation("io.ktor:ktor-shared-serialization-kotlinx-tests:$ktor_version")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("io.ktor:ktor-server-core:$ktor_version")
                implementation("io.ktor:ktor-server-netty:$ktor_version")
                implementation("ch.qos.logback:logback-classic:$logback_version")
                implementation("io.ktor:ktor-server-test-host:$ktor_version")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        all {
            languageSettings.apply {
                optIn("kotlin.RequiresOptIn")
            }
        }
    }

}

doPublish()

configureDokka(myModuleVersion = xmlutil_util_version)

idea {
    module {
        name = "xmlutil-ktor"
    }
}
