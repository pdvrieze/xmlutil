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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget


plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

kotlin {
    compilerOptions.jvmTarget = JvmTarget.JVM_11
}

java {
    targetCompatibility = JavaVersion.VERSION_11
}

gradlePlugin {
    plugins {
        register("projectPlugin") {
            id = "projectPlugin"
            implementationClass = "io.github.xmlutil.plugin.ProjectPlugin"
        }
    }
}

dependencies {
    implementation(kotlin("gradle-plugin", libs.versions.kotlin.get()))
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:${libs.versions.dokka.get()}")
}

repositories {
    mavenCentral()
    mavenLocal()
}
