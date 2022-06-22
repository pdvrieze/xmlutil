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

import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
    id(libs.plugins.kotlinMultiplatform.get().pluginId)/* version "1.7.0"*/ apply false
    idea
//    kotlin("multiplatform") apply false
    `maven-publish`
    signing
    id(libs.plugins.dokka.get().pluginId)
}

description = "The overall project for cross-platform xml access"

ext {
    set("myJavaVersion",JavaVersion.VERSION_1_8)
}

tasks {
    (findByName("wrapper") as? Wrapper)?.run {
        gradleVersion = "7.2"
    }
}

val xmlutil_version: String by project
val kotlin_version: String get() = libs.versions.kotlin.get()

allprojects {
    group = "io.github.pdvrieze.xmlutil"
    version = xmlutil_version
    repositories {
        maven {
            name = "Bundled maven"
            url = file("mavenBundled").toURI()
        }
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
        google()
    }
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:${kotlin_version}")
            force("org.jetbrains.kotlin:kotlin-reflect:${kotlin_version}")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${kotlin_version}")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlin_version}")
            force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0-RC")
        }
    }

    tasks.withType<KotlinNpmInstallTask> {
        args += "--ignore-scripts"
        dependsOn(":restoreYarnLock")
    }

}

rootProject.plugins.withType(YarnPlugin::class.java) {
    rootProject.the<YarnRootExtension>().disableGranularWorkspaces()
}

tasks.register("backupYarnLock") {
    dependsOn("kotlinNpmInstall")

    doLast {
        copy {
            from("$rootDir/build/js/yarn.lock")
            rename { "yarn.lock.bak" }
            into(rootDir)
        }
    }

    inputs.file("$rootDir/build/js/yarn.lock").withPropertyName("inputFile")
    outputs.file("$rootDir/yarn.lock.bak").withPropertyName("outputFile")
}

val restoreYarnLock = tasks.register("restoreYarnLock") {
    doLast {
        copy {
            from("$rootDir/yarn.lock.bak")
            rename { "yarn.lock" }
            into("$rootDir/build/js")
        }
    }

    inputs.file("$rootDir/yarn.lock.bak").withPropertyName("inputFile")
    outputs.file("$rootDir/build/js/yarn.lock").withPropertyName("outputFile")
}

//tasks.named("kotlinNpmInstall").configure {
//    dependsOn(restoreYarnLock)
//}

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
        isDownloadSources=true
        contentRoot = projectDir
    }
}
