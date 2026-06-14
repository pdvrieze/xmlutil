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

import net.devrieze.gradle.ext.addNativeTargets
import net.devrieze.gradle.ext.doPublish
import net.devrieze.gradle.ext.isKlibValidationEnabled
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JsMainFunctionExecutionMode
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.dokka)
    id("projectPlugin")
    kotlin("multiplatform")
    alias(libs.plugins.kotlinSerialization)
    `maven-publish`
    signing
    idea
}

config {
    applyLayout = true
    allWarningsAsErrors = false
}

kotlin {
    explicitApi()

    jvmToolchain(17)

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        keepLocallyUnsupportedTargets = false

        filters {
            exclude {
                annotatedWith.add("nl.adaptivity.xmlutil.XmlUtilInternal")
                byNames.apply {
                    add("nl.adaptivity.xmlutil.core.internal.**")
                    add("nl.adaptivity.xmlutil.core.impl.**")
                    add("nl.adaptivity.xmlutil.util.impl.**")
                }
            }
        }
        if (! isKlibValidationEnabled()) {
            checkTaskProvider.configure {
                enabled = false
            }
        }
    }

    val testTask = tasks.register("test") {
        group = "verification"
    }


    jvm {
        compilations.all {
            val targetTestTask = tasks.named<Test>("${target.name}Test")
            testTask.configure { dependsOn(targetTestTask) }
        }
        tasks.withType<Jar>().named(artifactsTaskName) {
            from(project.file("src/r8-workaround.pro")) {
                rename { "xmlutil-r8-workaround.pro" }
                into("META-INF/com.android.tools/r8")
            }
            from(project.file("src/jvmMain/proguard.pro")) {
                rename { "xmlutil-proguard.pro" }
                into("META-INF/proguard")
            }
        }

    }

    js {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            sourceMap = true
            sourceMapEmbedSources = JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS
            suppressWarnings = false
            verbose = true
            moduleKind = JsModuleKind.MODULE_UMD
            main = JsMainFunctionExecutionMode.CALL
        }
        browser()
        nodejs()
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.js.ExperimentalJsNoRuntime")
    }

    targets.all {
        @Suppress("OPT_IN_USAGE")
        when (val t = this) {
            is HasConfigurableKotlinCompilerOptions<*> -> t.compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.serialization.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
                implementation(projects.testutil)
                implementation(projects.serialization)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.junit.api)
                implementation(projects.coreJdk)

                runtimeOnly(libs.junit.engine)
                runtimeOnly(libs.woodstox)
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }

}

val cleanTestTask = tasks.register("cleanTest") {
    group = "verification"

    dependsOn(tasks.withType<Delete>().matching {
        it.name.startsWith("clean") && it.name.endsWith("Test")
    })
}

addNativeTargets()

doPublish("core")
