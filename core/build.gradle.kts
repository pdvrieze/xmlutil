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

import net.devrieze.gradle.ext.*
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

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
    archivesName.set("xmlutil")
    version = xmlutil_core_version
}

val autoModuleName = "net.devrieze.xmlutil.core"

kotlin {
    explicitApi()
    applyDefaultXmlUtilHierarchyTemplate()

    val testTask = tasks.create("test") {
        group = "verification"
    }
    val cleanTestTask = tasks.create("cleanTest") {
        group = "verification"
    }

    jvm {
        attributes {
            attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, envJvm)
        }
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
            from(project.file("src/jvmMain/proguard.pro")) {
                rename { "xmlutil-proguard.pro" }
                into("META-INF/proguard")
            }
        }

    }
    jvm("android") {
        attributes {
            attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, envAndroid)
        }
        compilations.all {
            tasks.named<Test>("${target.name}Test") {
                testTask.dependsOn(this)
            }
            cleanTestTask.dependsOn(tasks.named("clean${target.name[0].uppercaseChar()}${target.name.substring(1)}Test"))
        }

        tasks.withType<Jar>().named(artifactsTaskName) {
            from(project.file("src/r8-workaround.pro")) {
                rename { "xmlutil-r8-workaround.pro" }
                into("META-INF/com.android.tools/r8")
            }
            from(project.file("src/androidMain/proguard.pro")) {
                rename { "xmlutil-proguard.pro" }
                into("META-INF/com.android.tools/r8")
            }
            from(project.file("src/androidMain/proguard.pro")) {
                rename { "xmlutil-proguard.pro" }
                into("META-INF/com.android.tools/proguard")
            }
        }
    }
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
    @Suppress("OPT_IN_USAGE")
    wasmJs {
        nodejs()
        browser()
        compilations.all {
            kotlinOptions {
                sourceMap = true
                verbose = true
            }
        }
    }

    targets.all {
        mavenPublication {
            version = xmlutil_core_version
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
                implementation(libs.serialization.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
                implementation(project(":testutil"))
                implementation(project(":serialization"))
            }
        }

        val javaSharedMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
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

        val androidMain by getting {
            dependencies {
                compileOnly(libs.kxml2)
            }
        }

        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.junit5.api)

                runtimeOnly(libs.junit5.engine)
                runtimeOnly(libs.kxml2)
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
    sourceSets.all {
        languageSettings.apply {
            optIn("nl.adaptivity.xmlutil.XmlUtilInternal")
        }
    }

}

addNativeTargets()

apiValidation {
    nonPublicMarkers.add("nl.adaptivity.xmlutil.XmlUtilInternal")
    ignoredPackages.apply {
        add("nl.adaptivity.xmlutil.core.internal")
        add("nl.adaptivity.xmlutil.core.impl")
        add("nl.adaptivity.xmlutil.util.impl")
    }
}

doPublish()

configureDokka(myModuleVersion = xmlutil_core_version)
