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

@file:Suppress("PropertyName")

import net.devrieze.gradle.ext.addNativeTargets
import net.devrieze.gradle.ext.applyDefaultXmlUtilHierarchyTemplate
import net.devrieze.gradle.ext.doPublish
import net.devrieze.gradle.ext.envJvm
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.jetbrains.kotlin.gradle.dsl.JsMainFunctionExecutionMode
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("projectPlugin")
    kotlin("multiplatform")
    alias(libs.plugins.kotlinSerialization)
    `maven-publish`
    signing
    alias(libs.plugins.dokka)
    idea
//    alias(libs.plugins.binaryValidator)
}

base {
    archivesName = "xmlschema"
    description = "A simple library for serializing/deserializing xmlschema"
}

kotlin {
    applyDefaultXmlUtilHierarchyTemplate()
    jvm {
        attributes {
            attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, envJvm)
        }
    }
    js {
        browser()
        nodejs()

        compilerOptions {
            sourceMap = true
            sourceMapEmbedSources = JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS
            suppressWarnings = false
            verbose = true
            moduleKind = JsModuleKind.MODULE_UMD
            main = JsMainFunctionExecutionMode.NO_CALL
        }
    }

    targets.all {
        if (this is org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget) {
            testRuns.all {
                executionTask.configure {
                    useJUnitPlatform()
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
                api(project(":serialization"))
                implementation(libs.serialization.core)
                implementation(libs.datetime)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(projects.schemaTests)
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.serialization.json)
                implementation(project(":testutil"))
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
    }
}

addNativeTargets(includeWasm = false, includeWasi = false)

//doPublish()

config {
    dokkaModuleName = "xmlschema"
    kotlinApiVersion = KotlinVersion.KOTLIN_2_0
}

idea {
    module {
        name = "xmlutil-xmlschema"
    }
}
