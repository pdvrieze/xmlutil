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
import net.devrieze.gradle.ext.doPublish
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
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
    alias(libs.plugins.binaryValidator)
}

base {
    archivesName = "xmltestutil"
}

config {
    kotlinApiVersion = KotlinVersion.DEFAULT
}

val moduleName = "io.github.pdvrieze.testutil"

kotlin {
    jvm()

    js {
        browser()
        nodejs()
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
                api(libs.serialization.core)
                api(kotlin("test"))
                api(projects.core)

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

config {
    dokkaModuleName = "testutil"
}


idea {
    module {
        name = "xmlutil-testutil"
    }
}
