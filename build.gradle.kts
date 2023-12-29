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

import net.devrieze.gradle.ext.configureDokka
import net.devrieze.gradle.ext.envAndroid
import net.devrieze.gradle.ext.envJvm
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    id(libs.plugins.kotlinMultiplatform.get().pluginId) apply false
//    kotlin("multiplatform")/* version "1.7.0"*/ //apply false
    idea
//    kotlin("multiplatform") apply false
    `maven-publish`
    signing
    id(libs.plugins.dokka.get().pluginId)
}

description = "The overall project for cross-platform xml access"

ext {
    set("myJavaVersion", JavaVersion.VERSION_1_8)
}

val xmlutil_version: String by project
val kotlin_version: String get() = libs.versions.kotlin.get()

allprojects {
    val projectName = name
    group = "io.github.pdvrieze.xmlutil"
    version = xmlutil_version
    repositories {
        maven {
            name = "Bundled maven"
            url = file("mavenBundled").toURI()
        }
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")}
        google()
    }

    tasks.withType<KotlinNpmInstallTask> {
        args += "--ignore-scripts"
//        dependsOn(":restoreYarnLock")
    }

    extensions.findByType<JavaPluginExtension>()?.apply {
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
    afterEvaluate {

        extensions.findByType<JavaPluginExtension>()?.run {
            targetCompatibility = JavaVersion.VERSION_1_8
        }
        extensions.findByType<KotlinMultiplatformExtension>()?.run {
            targets.configureEach {
                if (this is KotlinJvmTarget) {
                    val targetName = name
                    attributes {
                        when (targetName) {
                            "jvm" -> {
                                attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, envJvm)
                                attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
                            }

                            "android" -> {
                                attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, envAndroid)
                                attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
                            }
                        }
                    }
                    compilations.configureEach {
                        compileTaskProvider.configure {
                            kotlinOptions {
                                jvmTarget = "1.8"
                            }
                        }
                    }
                }

            }
            sourceSets {
                all {
                    languageSettings {
                        progressiveMode = true
                        languageVersion = "1.9"

                        apiVersion = when(projectName) {
                            "xmlschema" -> "1.9"
                            else -> "1.8"
                        }
                        optIn("nl.adaptivity.xmlutil.ExperimentalXmlUtilApi")
                    }
                }
            }
        }
        extensions.findByType<KotlinJvmProjectExtension>()?.run {
            target {
                attributes {
                    attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, envJvm)
                    attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
                }
                sourceSets.configureEach {
                    languageSettings {
                        progressiveMode = true
                        languageVersion = "1.9"
                        apiVersion = "1.8"
                        optIn("nl.adaptivity.xmlutil.ExperimentalXmlUtilApi")
                    }
                }
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

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

project.configureDokka()

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
