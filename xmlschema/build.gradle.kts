/*
 * Copyright (c) 2021.
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
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlinSerialization)
    `maven-publish`
    signing
    id(libs.plugins.dokka.get().pluginId)
    idea
    alias(libs.plugins.binaryValidator)
}

val xmlutil_core_version: String by project
val xmlutil_versiondesc: String by project

base {
    archivesName.set("xmlschema")
    version = xmlutil_core_version
    description = "A simple library for serializing/deserializing xmlschema"
}

kotlin {
    targets {
        jvm {
            attributes {
                attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, envJvm)
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

    }
    targets.all {
        if (this is org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget) {
            testRuns.all {
                executionTask.configure {
                    useJUnitPlatform()
                }
            }
        }
        mavenPublication {
            version = xmlutil_core_version
        }
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
                api(project(":serialization"))
                implementation(libs.serialization.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.serialization.json)
                implementation(project(":testutil"))
            }
        }
        val javaShared by creating {
            dependsOn(commonMain)
        }

        val jvmMain by getting {
            dependsOn(javaShared)
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.junit5.api)

                implementation("org.xmlunit:xmlunit-core:2.6.0")

                implementation(kotlin("test-junit5"))

                runtimeOnly(libs.junit5.engine)
                runtimeOnly("com.fasterxml.woodstox:woodstox-core:5.1.0")
            }
        }
        all {
            languageSettings.apply {
                optIn("kotlin.RequiresOptIn")
            }
        }
    }
}

tasks.create<Task>("test") {
    dependsOn(tasks.named("jvmTest"))
    group="verification"
}

addNativeTargets()

apiValidation {
    ignoredPackages.apply {
//        add("nl.adaptivity.serialutil.impl")
    }

}

doPublish()

configureDokka(myModuleVersion = xmlutil_core_version)

idea {
    module {
        name = "xmlutil-serialutil"
    }
}
