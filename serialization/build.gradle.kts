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
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

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

base {
    archivesName.set("xmlutil-serialization")
    version = xmlutil_serial_version
}

val autoModuleName = "net.devrieze.xmlutil.serialization"


kotlin {
    applyDefaultXmlUtilHierarchyTemplate()
    explicitApi()

    jvm {
        compilations.all {
            tasks.named<Jar>("jvmJar") {
                manifest {
                    attributes("Automatic-Module-Name" to autoModuleName)
                }
            }
        }

        val woodstoxCompilation = compilations.create("woodstoxTest") {
            // This needs to be specified explicitly in 1.9.20
            compilerOptions.options.moduleName = "woodstoxTest"
            defaultSourceSet { }
        }
        testRuns.register("woodstoxTest") {
            setExecutionSourceFrom(
                listOf(compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)),
                listOf(woodstoxCompilation)
            )
        }

    }
//        androidTarget("actualAndroid")

    jvm("android")
    js {
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

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName="core-wasm"
        nodejs()
        browser {
            testTask {
                isEnabled = ! System.getenv().containsKey("GITHUB_ACTION")
            }
        }
    }

    targets.all {
        if (this is KotlinJvmTarget) {
            testRuns.all {
                executionTask.configure {
                    useJUnitPlatform()
                }
            }
        }
        mavenPublication {
            version = xmlutil_serial_version
        }
        compilations.all {
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + "-Xexpect-actual-classes"
            }
        }
    }


    sourceSets {

        val commonMain by getting {
            dependencies {
                api(project(":core"))

                api(libs.serialization.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(project(":serialutil"))
                implementation(project(":testutil"))
                implementation(libs.serialization.json)

                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }


        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
            }
        }

        val jvmMain by getting {}
        val commonJvmTest by getting {}
        val commonJvmMain by getting {}
        val jvmWoodstoxTest by getting {
            dependsOn(commonJvmTest)
            dependsOn(commonJvmMain)
            dependencies {
                implementation(kotlin("test-junit5"))
                runtimeOnly(libs.junit5.engine)
                runtimeOnly(libs.woodstox)
            }
        }

        val androidMain by getting {
            dependencies {
                compileOnly(libs.kxml2)
                api(project(":core")) {
                    attributes { attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm) }
                }
            }
        }

        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                runtimeOnly(libs.kxml2)

                implementation(libs.junit5.api)
                implementation(libs.kotlin.reflect)

                runtimeOnly(libs.junit5.engine)
            }
        }

        val jsMain by getting {
            dependencies {
                api(project(":core"))
            }
        }

        val jsTest by getting {
            languageSettings.enableLanguageFeature("InlineClasses")

            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        all {
            if (this.name == "nativeMain") {
                dependencies {
                    api(project(":core"))
                }
            }
            if (System.getProperty("idea.active") == "true" && name == "nativeTest") { // Hackery to get at the native source sets that shouldn't be needed
                languageSettings.enableLanguageFeature("InlineClasses")
                dependencies {
                    implementation(kotlin("test-common"))
                    implementation(kotlin("test-annotations-common"))
                }

//                dependsOn(this@sourceSets.get("nativeMain"))
//                dependsOn(inlineSupportTest)
            }
            languageSettings.apply {
                optIn("nl.adaptivity.xmlutil.XmlUtilInternal")
            }
        }
    }

}

addNativeTargets()

dependencies {
    attributesSchema {
        attribute(KotlinPlatformType.attribute)
    }
}

apiValidation {
    nonPublicMarkers.apply {
        add("nl.adaptivity.xmlutil.serialization.WillBePrivate")
        add("nl.adaptivity.xmlutil.XmlUtilInternal")
    }
    ignoredPackages.apply {
        add("nl.adaptivity.xmlutil.serialization.impl")
    }

}

doPublish()

configureDokka(myModuleVersion = xmlutil_serial_version)

tasks.register("cleanTest") {
    group = "verification"
    dependsOn(tasks.named("cleanAllTests"))
}

tasks.withType<Test> {
    logger.lifecycle("Enabling xml reports on task ${project.name}:${name}")
    reports {
        junitXml.required.set(true)
    }
}

idea {
    this.module.name = "xmlutil-serialization"
}
