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
import net.devrieze.gradle.ext.configureDokka
import net.devrieze.gradle.ext.doPublish
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlinSerialization)
    `maven-publish`
    signing
    id(libs.plugins.dokka.get().pluginId)
    idea
    alias(libs.plugins.binaryValidator)
}

val xmlutil_serial_version: String by project
val xmlutil_core_version: String by project
val xmlutil_util_version: String by project
val xmlutil_versiondesc: String by project

base {
    archivesName.set("xmltestutil")
    version = xmlutil_core_version
}

val moduleName = "io.github.pdvrieze.testutil"

kotlin {
    jvm {
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
                isEnabled = ! System.getenv().containsKey("GITHUB_ACTION")
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.serialization.core)
                api(kotlin("test"))
                api(project(":core"))

            }
        }

        all {
            languageSettings.apply {
                optIn("nl.adaptivity.xmlutil.ExperimentalXmlUtilApi")
                optIn("nl.adaptivity.xmlutil.XmlUtilInternal")
            }
        }
    }

}

addNativeTargets()

doPublish()

configureDokka("testutil", xmlutil_core_version)

idea {
    module {
        name = "xmlutil-testutil"
    }
}
