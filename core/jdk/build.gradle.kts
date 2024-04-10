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

import net.devrieze.gradle.ext.addNativeTargets
import net.devrieze.gradle.ext.applyDefaultXmlUtilHierarchyTemplate
import net.devrieze.gradle.ext.doPublish
import net.devrieze.gradle.ext.envJvm
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE

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
    archivesName.set("xmlutil")
}

val autoModuleName = "net.devrieze.xmlutil.core.jvm"

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
            kotlinOptions {
                freeCompilerArgs += "-Xjvm-default=all"
            }

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

    targets.all {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs += "-Xexpect-actual-classes"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core:base"))
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


        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.junit5.api)

                runtimeOnly(libs.junit5.engine)
                runtimeOnly(libs.woodstox)
            }
        }
    }
    sourceSets.all {
        languageSettings.apply {
            optIn("nl.adaptivity.xmlutil.XmlUtilInternal")
            optIn("nl.adaptivity.xmlutil.XmlUtilDeprecatedInternal")
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

doPublish("core-jdk")
