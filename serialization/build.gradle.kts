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
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JsMainFunctionExecutionMode
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

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
    archivesName = "serialization"
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

        /*
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
        */

    }

    js {
        browser()
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

    targets.all {
        if (this is KotlinJvmTarget) {
            testRuns.all {
                executionTask.configure {
                    useJUnitPlatform()
                }
            }
        }
        @Suppress("OPT_IN_USAGE")
        if (this is HasConfigurableKotlinCompilerOptions<*>) {
            compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }


    sourceSets {

        val commonMain by getting {
            dependencies {
                api(projects.core)

                api(libs.serialization.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(projects.serialutil)
                implementation(projects.testutil)
                implementation(libs.serialization.json)

                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }


        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(projects.coreJdk)
            }
        }

        val jvmMain by getting {
            dependencies {
                runtimeOnly(projects.core)
            }
        }
        val commonJvmMain by getting {}
        /*
        val jvmWoodstoxTest by getting {
            dependsOn(commonJvmTest)
            dependsOn(commonJvmMain)
            dependencies {
                implementation(kotlin("test-junit5"))
                runtimeOnly(libs.junit5.engine)
                runtimeOnly(libs.woodstox)
            }
        }
        */

        val jsMain by getting {
            dependencies {
                api(projects.core)
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
                    api(projects.core)
                    implementation(libs.kotlinx.atomicfu)
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

config {
    createAndroidCompatComponent = true
}

addNativeTargets()

dependencies {
    attributesSchema {
        attribute(KotlinPlatformType.attribute)
    }
}

apiValidation {
    @Suppress("OPT_IN_USAGE")
    klib {
        strictValidation = false
        enabled = true
    }
    nonPublicMarkers.apply {
        add("nl.adaptivity.xmlutil.serialization.WillBePrivate")
        add("nl.adaptivity.xmlutil.XmlUtilInternal")
    }
    ignoredPackages.apply {
        add("nl.adaptivity.xmlutil.serialization.impl")
    }

}

doPublish()

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
