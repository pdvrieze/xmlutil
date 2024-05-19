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
import net.devrieze.gradle.ext.doPublish
import net.devrieze.gradle.ext.envAndroid
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE

plugins {
    id("projectPlugin")
    kotlin("jvm")
    alias(libs.plugins.kotlinSerialization)
    `java-library`
    `maven-publish`
    signing
    alias(libs.plugins.dokka)
    idea
    alias(libs.plugins.binaryValidator)
}

base {
    archivesName = "core-android"
}

val autoModuleName = "net.devrieze.xmlutil.core"

kotlin {
    explicitApi()

    target {
        attributes {
            attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, envAndroid)
        }

        tasks.withType<Jar>().named(artifactsTaskName) {
            from(project.file("src/main/proguard.pro")) {
                rename { "xmlutil-proguard.pro" }
                into("META-INF/com.android.tools/r8")
            }
            from(project.file("src/main/proguard.pro")) {
                rename { "xmlutil-proguard.pro" }
                into("META-INF/com.android.tools/proguard")
            }
        }
    }
}

dependencies {
    compileOnly(libs.kxml2)
    api(project(":core:base"))

    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.junit5.api)

    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.kxml2)
}

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

publishing {
    publications {
        create<MavenPublication>("mavenKotlin") {
            artifactId = "core-android"
            from(components["java"])
            artifact(tasks["kotlinSourcesJar"])
        }
    }
}

doPublish("core-android")
