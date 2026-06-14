/*
 * Copyright (c) 2024-2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

@file:Suppress("PropertyName")

import net.devrieze.gradle.ext.addNativeTargets
import net.devrieze.gradle.ext.doPublish
import net.devrieze.gradle.ext.isKlibValidationEnabled
import org.jetbrains.kotlin.gradle.dsl.JsMainFunctionExecutionMode
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("projectPlugin")
    kotlin("multiplatform")
    alias(libs.plugins.kotlinSerialization)
    `maven-publish`
    signing
    alias(libs.plugins.dokka)
    idea
}

base {
    archivesName = "serialutil"
}

config {
    createAndroidCompatComponent = true
    dokkaModuleName = "serialutil"
    optIns = emptyList()
}

kotlin {

    jvmToolchain(17)

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {

        filters {
            exclude {
                annotatedWith.add("nl.adaptivity.xmlutil.XmlUtilInternal")
                byNames.apply {
                    add("nl.adaptivity.serialutil.impl.**")
                }
            }
        }

        if (! isKlibValidationEnabled()) {
            checkTaskProvider.configure {
                enabled = false
            }
        }

    }

    jvm()

    js {
        browser()
        nodejs()
        @Suppress("OPT_IN_USAGE")
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
                compileOnly(projects.core) // for optin markers
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
                implementation(libs.junit.api)

                runtimeOnly(libs.junit.engine)
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

doPublish()

idea {
    module {
        name = "xmlutil-serialutil"
    }
}
