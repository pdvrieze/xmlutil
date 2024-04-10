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

import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("projectPlugin")
    alias(libs.plugins.kotlinMultiplatform) apply false
//    kotlin("multiplatform")/* version "1.7.0"*/ //apply false
    idea
//    kotlin("multiplatform") apply false
    `maven-publish`
    signing
    alias(libs.plugins.dokka)
}

description = "The overall project for cross-platform xml access"

val xmlutil_version: String by project
val kotlin_version: String get() = libs.versions.kotlin.get()

plugins.withType<NodeJsRootPlugin> {
    extensions.configure<NodeJsRootExtension> {
//        nodeVersion = "21.2.0"
        // This version is needed to be able to use/test/run the latest wasm opcodes
        nodeVersion = "21.0.0-v8-canary202309143a48826a08"
        nodeDownloadBaseUrl = "https://nodejs.org/download/v8-canary"
    }
}

tasks.withType<KotlinNpmInstallTask>().configureEach {
    args.add("--ignore-engines")
}

afterEvaluate {
    rootProject.plugins.withType(YarnPlugin::class.java) {
        rootProject.the<YarnRootExtension>().apply {
//            resolution("minimist", "1.2.6")
//            resolution("webpack", "5.76.0")
//            resolution("qs", "6.11.0")
//            resolution("follow-redirects", "1.14.8")
        }
    }
}

tasks.register<Copy>("pages") {
    group="documentation"
    dependsOn(tasks.named("dokkaHtmlMultiModule"))
    into(projectDir.resolve("pages"))
    from(tasks.named<DokkaMultiModuleTask>("dokkaHtmlMultiModule").flatMap { it.outputDirectory })
    // Needed as pages may have content already
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

configurations.all {
    resolutionStrategy {
        force("org.apache.httpcomponents:httpclient:4.5.9")
        force("org.apache.httpcomponents:httpcore:4.4.11")
    }
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    publishOnFailureIf("true".equals(System.getenv("TRAVIS")))
}

idea {
    project {
        languageLevel = IdeaLanguageLevel(JavaVersion.VERSION_1_8)
    }
    module {
        isDownloadSources = true
        contentRoot = projectDir
    }
}
