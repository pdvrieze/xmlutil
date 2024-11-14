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
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions

plugins {
    alias(libs.plugins.dokka)
    id("projectPlugin")
    kotlin("multiplatform")
    `maven-publish`
    signing
    idea
}

val autoModuleName = "net.devrieze.xmlutil.serialization.kxio"

kotlin {
    explicitApi()
    applyDefaultXmlUtilHierarchyTemplate()

    jvm()
    js {
        browser()
        compilerOptions {
            sourceMap = true
            verbose = true
        }
    }

    targets.all {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        if (this is HasConfigurableKotlinCompilerOptions<*>) {
            compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.serialization)
//                api(projects.core.kxio)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
                implementation(projects.testutil)
                implementation(projects.serialization)
            }
        }
    }
}

addNativeTargets()

doPublish("serialization-io")
