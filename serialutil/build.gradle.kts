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

@file:Suppress("PropertyName")

import net.devrieze.gradle.ext.addNativeTargets
import net.devrieze.gradle.ext.applyDefaultXmlUtilHierarchyTemplate
import net.devrieze.gradle.ext.configureDokka
import net.devrieze.gradle.ext.doPublish
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlinSerialization)
    id("maven-publish")
    signing
    idea
    id("org.jetbrains.dokka")
    alias(libs.plugins.binaryValidator)
}

val xmlutil_util_version: String by project

base {
    archivesName.set("serialutil")
    version = xmlutil_util_version
}

val autoModuleName = "net.devrieze.serialutil"

kotlin {
    applyDefaultXmlUtilHierarchyTemplate()
    jvm {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs += "-Xjvm-default=all"
            }
        }
    }
    jvm("android") {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs += "-Xjvm-default=all"
            }
        }
    }
    js {
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

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        browser {
            testTask {
                isEnabled = !System.getenv().containsKey("GITHUB_ACTION")
            }
        }
    }

    targets.all {
        mavenPublication {
            version = xmlutil_util_version
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.serialization.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.serialization.json)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.junit5.api)

                runtimeOnly(libs.junit5.engine)
            }
        }

        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.junit5.api)

                runtimeOnly(libs.junit5.engine)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

addNativeTargets()

apiValidation {
    ignoredPackages.apply {
        add("nl.adaptivity.serialutil.impl")
    }

}

doPublish()

configureDokka(myModuleVersion = xmlutil_util_version)

idea {
    module {
        name = "xmlutil-serialutil"
    }
}
