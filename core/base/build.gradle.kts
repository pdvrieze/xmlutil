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

import kotlinx.validation.ExperimentalBCVApi
import net.devrieze.gradle.ext.addNativeTargets
import net.devrieze.gradle.ext.applyDefaultXmlUtilHierarchyTemplate
import net.devrieze.gradle.ext.doPublish
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JsMainFunctionExecutionMode
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

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

config {
    applyLayout = false
}

val autoModuleName = "net.devrieze.xmlutil.core"

kotlin {
    applyDefaultXmlUtilHierarchyTemplate()
    explicitApi()

    components.configureEach {
        val c: SoftwareComponent = this

        logger.lifecycle("Found component ${c.name}")
    }

    val testTask = tasks.create("test") {
        group = "verification"
    }
    val cleanTestTask = tasks.create("cleanTest") {
        group = "verification"
    }

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
        val targetName = name
        mavenPublication {
            when (targetName) {
                "jvm" -> artifactId = "core-jvmCommon"
            }
        }
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
                implementation(libs.junit5.api)

                runtimeOnly(libs.junit5.engine)
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

publishing {
    publications.withType<MavenPublication>().named("kotlinMultiplatform") {
        artifactId = "core-base"
    }
}

addNativeTargets()

apiValidation {
    @OptIn(ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
    nonPublicMarkers.add("nl.adaptivity.xmlutil.XmlUtilInternal")
    ignoredPackages.apply {
        add("nl.adaptivity.xmlutil.core.internal")
        add("nl.adaptivity.xmlutil.core.impl")
        add("nl.adaptivity.xmlutil.util.impl")
    }
}

doPublish("core")
