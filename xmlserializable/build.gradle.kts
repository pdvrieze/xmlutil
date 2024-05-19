/*
 * Copyright (c) 2024.
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
import net.devrieze.gradle.ext.doPublish
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JsMainFunctionExecutionMode
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode

plugins {
    id("projectPlugin")
    kotlin("multiplatform")
    alias(libs.plugins.kotlinSerialization)
    `maven-publish`
    signing
    alias(libs.plugins.dokka)
    idea
    alias(libs.plugins.binaryValidator)
}

base {
    archivesName = "xmlserializable"
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
            cleanTestTask.dependsOn(tasks.getByName("clean${target.name[0].uppercaseChar()}${target.name.substring(1)}Test"))
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
            cleanTestTask.dependsOn(tasks.named("clean${target.name[0].uppercaseChar()}${target.name.substring(1)}Test"))
        }
    }
    js {
        browser()

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            sourceMap = true
            sourceMapEmbedSources = JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS
            suppressWarnings = false
            verbose = true
            moduleKind = JsModuleKind.MODULE_UMD
            main = JsMainFunctionExecutionMode.CALL
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core:base")) // Don't add a runtime dep here
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

idea {
    module {
        name = "xmlutil-xmlserializable"
    }
}
