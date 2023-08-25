/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

import net.devrieze.gradle.ext.*
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlinSerialization)
    `maven-publish`
    signing
    id("org.jetbrains.dokka")
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
    explicitApi()
    targets {
        jvm {
            compilations.all {
                tasks.named<Jar>("jvmJar") {
                    manifest {
                        attributes("Automatic-Module-Name" to autoModuleName)
                    }
                }
            }

            val woodstoxCompilation = compilations.register("woodstoxTest")
            val woodstoxTestRun = testRuns.create("woodstoxTest") {
                setExecutionSourceFrom(
                    listOf(compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)),
                    listOf(
                        woodstoxCompilation.get()
                    )
                )
            }


        }
        jvm("android")
        js(BOTH) {
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

        val inlineSupportTest by creating {
            dependsOn(commonMain)
            dependsOn(commonTest)
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val javaShared by creating {
            dependsOn(commonMain)
        }

        val javaSharedTest by creating {
            dependsOn(inlineSupportTest)
            dependsOn(javaShared)
            dependsOn(commonTest)
        }

        val jvmMain by getting {
            dependsOn(javaShared)
        }

        val jvmTestCommon by creating {
            dependsOn(javaSharedTest)
            dependsOn(jvmMain)

            dependencies {
//                implementation(kotlin("test-junit5"))
                implementation(libs.junit5.api)

                runtimeOnly(libs.junit5.engine)
                implementation(libs.kotlin.reflect)
            }
        }

        val jvmTest by getting {
            dependsOn(jvmTestCommon)
            dependencies {
                implementation(kotlin("test-junit5"))
            }
        }
        val jvmWoodstoxTest by getting {
            dependsOn(jvmTestCommon)
            dependencies {
                implementation(kotlin("test-junit5"))
                runtimeOnly(libs.junit5.engine)
                runtimeOnly(libs.woodstox)
            }
        }

        val androidMain by getting {
            dependsOn(javaShared)

            dependencies {
                compileOnly(libs.kxml2)
            }
        }

        val androidTest by getting {
            dependsOn(javaSharedTest)
            dependsOn(androidMain)

            dependencies {
                implementation(kotlin("test-junit5"))
                runtimeOnly(libs.kxml2)

                implementation(libs.junit5.api)
                implementation(libs.kotlin.reflect)

                runtimeOnly(libs.junit5.engine)
            }
        }

        val jsMain by getting {
        }

        val jsTest by getting {
            languageSettings.enableLanguageFeature("InlineClasses")

            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        all {
            if (System.getProperty("idea.active") == "true" && name == "nativeTest") { // Hackery to get at the native source sets that shouldn't be needed
                languageSettings.enableLanguageFeature("InlineClasses")
                dependencies {
                    implementation(kotlin("test-common"))
                    implementation(kotlin("test-annotations-common"))
                }

                dependsOn(this@sourceSets.get("nativeMain"))
                dependsOn(inlineSupportTest)
            }
            languageSettings.apply {
                optIn("nl.adaptivity.xmlutil.XmlUtilInternal")
            }
        }
    }

}

addNativeTargets()

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

tasks.named<KotlinJsTest>("jsLegacyBrowserTest") {
    filter.excludeTestsMatching("nl.adaptivity.xml.serialization.OrderedFieldsTest")
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
