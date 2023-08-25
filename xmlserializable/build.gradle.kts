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

import net.devrieze.gradle.ext.configureDokka
import net.devrieze.gradle.ext.doPublish
import net.devrieze.gradle.ext.envAndroid
import net.devrieze.gradle.ext.envJvm
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    targets {
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
        js(BOTH) {
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

        val javaShared by creating {
            dependsOn(commonMain)
        }

        val jvmMain by getting {
            dependsOn(javaShared)
        }

        val jvmTest by getting {
            dependencies {
                dependsOn(commonTest)
                implementation(kotlin("test-junit5"))
                implementation(libs.junit5.api)

                runtimeOnly(libs.junit5.engine)
                runtimeOnly(libs.woodstox)
            }
        }

        val androidMain by getting {
            dependsOn(javaShared)

            dependencies {
                compileOnly(libs.kxml2)
            }
        }

        val androidTest by getting {
            dependencies {
                dependsOn(commonTest)

                implementation(kotlin("test-junit5"))
                implementation(libs.junit5.api)

                runtimeOnly(libs.junit5.engine)
                runtimeOnly(libs.kxml2)
            }
        }

        val jsMain by getting {
            dependsOn(commonMain)
        }

        val jsTest by getting {
            dependsOn(commonTest)
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

    }
    sourceSets.all {
        languageSettings {
            optIn("nl.adaptivity.xmlutil.XmlUtilInternal")
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
