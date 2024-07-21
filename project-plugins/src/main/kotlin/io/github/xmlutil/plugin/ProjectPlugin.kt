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
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

class ProjectPlugin: Plugin<Project> {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    override fun apply(project: Project) {
        project.logger.info("===================\nUsing ProjectPlugin\n===================")

        val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
        val xmlutil_version = libs.findVersion("xmlutil").get().requiredVersion

        project.group = "io.github.pdvrieze.xmlutil"
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
            dokkaModuleName.convention(project.provider { project.name })
            dokkaVersion.convention(project.provider { project.version.toString() })
            applyLayout.convention(true)
            kotlinApiVersion.convention(KotlinVersion.KOTLIN_1_8)
        }
        project.plugins.all {
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
                            apiVersion = e.kotlinApiVersion
                            configureCompilerOptions(project, "project ${project.name}")
                        }

                        sourceSets.configureEach {
                            languageSettings {
                                configureOptins()
                            }
                        }
                        target {
                            attributes {
                                attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.envJvm)
                                attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
                            }
                            mavenPublication {
                                version = xmlutil_version
                                project.logger.info("Setting maven publication ($artifactId) version to $xmlutil_version")
                            }
                        }
                    }

                }

                is KotlinMultiplatformPluginWrapper -> {
                    project.the<KotlinMultiplatformExtension>().apply {
                        if(e.applyLayout.get()) applyDefaultXmlUtilHierarchyTemplate()
                        compilerOptions {
                            configureCompilerOptions(project, "project ${project.name}")
                        }
                        targets.configureEach {
                            val isJvm = this is KotlinJvmTarget
                            this.compilations.configureEach {
                                compileTaskProvider.configure {
                                    compilerOptions {
                                        when {
                                            isJvm -> apiVersion = e.kotlinApiVersion
                                            else -> apiVersion = KotlinVersion.KOTLIN_2_0
                                        }
                                    }
                                }
                            }
                            mavenPublication {
                                version = xmlutil_version
                                project.logger.info("Setting maven publication ($artifactId) version to $xmlutil_version")
                            }
                        }

/*
                        metadata {
                            mavenPublication {
                                version = xmlutil_version
                            }
                        }
*/

                        targets.withType<KotlinJvmTarget> {
                            compilations.configureEach {
                                compileTaskProvider.configure {
                                    compilerOptions {
                                        configureCompilerOptions(project, "${project.name}:$name")
                                    }
                                }
                            }
                            when (name) {
                                "jdk",
                                "jvm" -> attributes {
                                    project.logger.debug("Setting attributes for target jvm")
                                    attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.envJvm)
                                    attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
                                }

                                "android" -> attributes {
                                    project.logger.debug("Setting attributes for target android")
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

    private fun KotlinCommonCompilerOptions.configureCompilerOptions(project: Project, name: String) {
        progressiveMode = true
        languageVersion = KotlinVersion.KOTLIN_2_0
        configureOptins()
        if (this is KotlinJvmCompilerOptions) {
            project.logger.info("Setting common compilation options for $name")
            jvmTarget = JvmTarget.JVM_1_8
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }

    private fun LanguageSettingsBuilder.configureOptins() {
        optIn("nl.adaptivity.xmlutil.ExperimentalXmlUtilApi")
        optIn("nl.adaptivity.xmlutil.XmlUtilInternal")
        optIn("nl.adaptivity.xmlutil.XmlUtilDeprecatedInternal")
    }

    private fun KotlinCommonCompilerOptions.configureOptins() {
        optIn.add("nl.adaptivity.xmlutil.ExperimentalXmlUtilApi")
        optIn.add("nl.adaptivity.xmlutil.XmlUtilInternal")
        optIn.add("nl.adaptivity.xmlutil.XmlUtilDeprecatedInternal")
    }
}

abstract class ProjectConfigurationExtension {
    abstract val dokkaModuleName: Property<String>
    abstract val dokkaVersion: Property<String>
    abstract val applyLayout: Property<Boolean>
    abstract val kotlinApiVersion: Property<KotlinVersion>
}
