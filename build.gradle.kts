/*
 * Copyright (c) 2024-2025.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("projectPlugin")
    alias(libs.plugins.kotlinMultiplatform) apply false
    idea
    `maven-publish`
    signing
    alias(libs.plugins.dokka)
}

description = "The overall project for cross-platform xml access"

val xmlutil_version: String by project
val kotlin_version: String get() = libs.versions.kotlin.get()

tasks.withType<KotlinNpmInstallTask>().configureEach {
    args.add("--ignore-engines")
}

tasks.register<Copy>("pages") {
    group="documentation"
    val dokkaTasks = tasks.named<DokkaGenerateTask>("dokkaGeneratePublicationHtml")
    dependsOn(dokkaTasks)
    into(projectDir.resolve("pages"))
    from(dokkaTasks.flatMap { it.outputDirectory })
    // Needed as pages may have content already
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    publishOnFailureIf("true".equals(System.getenv("TRAVIS")))
}

idea {
    module {
        isDownloadSources = true
        contentRoot = projectDir
    }
}

dependencies {
    dokka(projects.core)
    dokka(projects.coreJdk)
    dokka(projects.coreAndroid)
    dokka(projects.serialization)
    dokka(projects.serialutil)
}
