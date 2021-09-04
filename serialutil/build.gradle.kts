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

import net.devrieze.gradle.ext.configureDokka
import net.devrieze.gradle.ext.doPublish
import net.devrieze.gradle.ext.envAndroid
import net.devrieze.gradle.ext.envJvm
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("kotlinx-serialization")
    id("maven-publish")
    id("signing")
    idea
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

val xmlutil_serial_version: String by project
val xmlutil_core_version: String by project
val xmlutil_util_version: String by project
val xmlutil_versiondesc: String by project

base {
    archivesBaseName = "serialutil"
    version = xmlutil_util_version
}

val serializationVersion: String by project

val jupiterVersion: String by project
val kotlin_version: String by project

val androidAttribute = Attribute.of("net.devrieze.android", Boolean::class.javaObjectType)

val moduleName = "net.devrieze.serialutil"

kotlin {
    targets {
        jvm {
            attributes {
                attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, envJvm)
                attribute(androidAttribute, false)
            }
            compilations.all {
                compileKotlinTaskProvider.configure {
                    kotlinOptions {
                        jvmTarget = "1.8"
                    }
                }
            }
        }
        jvm("android") {
            attributes {
                attribute(androidAttribute, true)
                attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, envAndroid)
                attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
            }
            compilations.all {
                kotlinOptions {
                    jvmTarget = if (name=="test") "1.8" else "1.6"
                }
            }
        }
        js(BOTH) {
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
            compilations.named("test") {
                kotlinOptions.useIR = true
            }
            testRuns.all {
                executionTask.configure {
                    useJUnitPlatform()
                }
            }
        }
        mavenPublication {
            version = xmlutil_util_version
        }
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            }
        }
        val javaShared by creating {
            dependsOn(commonMain)
        }

        val jvmMain by getting {
            dependsOn(javaShared)
            dependencies {
                implementation(kotlin("stdlib-jdk8", kotlin_version))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")

                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
            }
        }
        val androidMain by getting {
            dependsOn(javaShared)
        }

        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")

                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        all {
            languageSettings.apply {
                useExperimentalAnnotation("kotlin.RequiresOptIn")
            }
        }
    }

}
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
