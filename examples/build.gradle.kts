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

import net.devrieze.gradle.ext.envJvm
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinSerialization)
    idea
}

base {
    archivesName.set("examples")
}

val serializationVersion: String by project

val kotlin_version: String get() = libs.versions.kotlin.get()

val autoModuleName = "net.devrieze.serialexamples"

java {
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    sourceSets.all {
        languageSettings {
            optIn("kotlin.RequiresOptIn")
        }
    }
    target {
        attributes {
            attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, envJvm)
        }
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
}

dependencies {
    implementation(project(":serialization"))
    implementation(project(":serialutil"))
}

idea {
    module {
        name = "xmlutil-examples"
    }
}
