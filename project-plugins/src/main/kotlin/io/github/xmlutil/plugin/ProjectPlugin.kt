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

package io.github.xmlutil.plugin

import net.devrieze.gradle.ext.applyDefaultXmlUtilHierarchyTemplate
import net.devrieze.gradle.ext.configureDokka
import net.devrieze.gradle.ext.envAndroid
import net.devrieze.gradle.ext.envJvm
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

class ProjectPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.logger.lifecycle("  =======\nUsing ProjectPlugin\n  =======")
        project.group = "io.github.pdvrieze.xmlutil"
        val xmlutil_version: String by project
        project.version = xmlutil_version
        project.tasks.withType<KotlinNpmInstallTask> {
            args += "--ignore-scripts"
        }
        project.tasks.withType<Test> {
            useJUnitPlatform()
        }
        project.repositories {
            mavenCentral()
            mavenLocal()
        }

        val e = project.extensions.create<ProjectConfigurationExtension>("config").apply {
            dokkaModuleName.convention(project.name)
            dokkaVersion.convention(project.version.toString())
        }
        project.plugins.all {
            project.logger.lifecycle(" --> Configuring plugin ${this.javaClass.name} for project ${project.name}")
            when (this) {
                is JavaPlugin -> {
                    project.extensions.configure<JavaPluginExtension> {
                        toolchain {
                            languageVersion.set(JavaLanguageVersion.of(11))
                        }
                        targetCompatibility = JavaVersion.VERSION_1_8
                        sourceCompatibility = JavaVersion.VERSION_1_8
                    }
                }

                is KotlinPluginWrapper -> {
                    project.extensions.configure<KotlinJvmProjectExtension> {
                        compilerOptions {
                            jvmTarget = JvmTarget.JVM_1_8
                            progressiveMode = true
                            languageVersion = KotlinVersion.KOTLIN_1_8
                        }
                        target {
                            attributes {
                                attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.envJvm)
                                attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
                            }
                            sourceSets.configureEach {
                                languageSettings {
                                    optIn("nl.adaptivity.xmlutil.ExperimentalXmlUtilApi")
                                }
                            }
                        }
                    }

                }

                is KotlinMultiplatformPluginWrapper -> {
                    project.the<KotlinMultiplatformExtension>().apply {
                        applyDefaultXmlUtilHierarchyTemplate()
                        targets.configureEach {
                            val isJvm = this is KotlinJvmTarget
                            sourceSets.configureEach {
                                languageSettings {
                                    progressiveMode = true
                                    languageVersion = "1.9"
                                    apiVersion = if (isJvm) "1.8" else "1.9"
                                    optIn("nl.adaptivity.xmlutil.ExperimentalXmlUtilApi")
                                }
                            }
                        }

                        targets.withType<KotlinJvmTarget> {
                            compilations.configureEach {
                                compileTaskProvider.configure {
                                    compilerOptions {
                                        jvmTarget = JvmTarget.JVM_1_8
                                    }
                                }
                            }
                            when (name) {
                                "jvm" -> attributes {
                                    project.logger.lifecycle("Setting attributes for target jvm")
                                    attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.envJvm)
                                    attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
                                }

                                "android" -> attributes {
                                    project.logger.lifecycle("Setting attributes for target android")
                                    attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.envAndroid)
                                    attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
                                }

                                else -> project.logger.error("Unsupported target name: $name")
                            }

                        }
                    }

                }
            }
        }
        project.plugins.apply(DokkaPlugin::class.java)
        project.configureDokka(e.dokkaModuleName, e.dokkaVersion)
    }
}

abstract class ProjectConfigurationExtension {
    abstract val dokkaModuleName: Property<String>
    abstract val dokkaVersion: Property<String>
}
