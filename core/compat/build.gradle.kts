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
    id("projectPlugin")
    kotlin("multiplatform")
    `maven-publish`
    signing
    alias(libs.plugins.dokka)
    idea
}

val autoModuleName = "net.devrieze.xmlutil.core.compat"

kotlin {
    explicitApi()
    applyDefaultXmlUtilHierarchyTemplate()

    jvm("jdk")
    jvm("android")
    js {
        browser()
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
                api(projects.core)
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

        val jdkMain by getting {
            dependencies {
                api(projects.coreJdk)
            }
        }

        val androidMain by getting {
            dependencies {
                api(projects.coreAndroid)
            }
        }
    }
}

addNativeTargets()

doPublish("compat")
