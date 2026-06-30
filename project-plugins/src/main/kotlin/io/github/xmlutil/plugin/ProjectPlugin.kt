/*
 * Copyright (c) 2024-2026.
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
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.register
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProjectPlugin: Plugin<Project> {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    override fun apply(project: Project) {
        project.logger.info("===================\nUsing ProjectPlugin\n===================")

        val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
        val xmlutil_version = libs.findVersion("xmlutil").get().requiredVersion

        project.group = "io.github.pdvrieze.xmlutil"
        project.version = xmlutil_version


        when {
            project.isSnapshot -> project.logger.debug("Project release is a snapshot release {}", project.version)
            else -> project.logger.debug("Project release is not a snapshot release {}", project.version)
        }

        if (project == project.rootProject) {
            val repositoryDir = project.layout.buildDirectory.dir("project-local-repository")



            val cleanLocalRepoTask = project.tasks.register("cleanLocalRepo") {
                doFirst {
                    if (repositoryDir.isPresent) {
                        repositoryDir.get().asFile.deleteRecursively()
                    }
                }
            }

            val collateTask = project.tasks.register<Zip>("collateModuleRepositories") {
                group = PublishingPlugin.PUBLISH_TASK_GROUP
                description = "Zip task that collates all local repositories into a single zip file"
                destinationDirectory = project.layout.buildDirectory.dir("repositoryArchive")
                archiveBaseName = "${project.name}-publishing"

                from(repositoryDir) {
                    exclude { ".asc." in it.name }
                    exclude { it.name.startsWith("maven-metadata.xml") }
//                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                }

                project.subprojects {
                    val publishTasks = tasks.matching { it is PublishToMavenRepository && it.repository?.name == "projectLocal" }
                    logger.debug("Adding local publication tasks for subproject ${path} as dependency to collateModuleRepositories")
                    dependsOn(publishTasks)
                }

            }

            project.tasks.register<PublishToSonatypeTask>("publishToSonatype") {
                group = PublishingPlugin.PUBLISH_TASK_GROUP
                description = "Publish the repositories to the sonatype maven central portal"

                from(collateTask.flatMap { t -> t.archiveFile.map { it.asFile } })
            }

        }



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
            dokkaOverrideTarget.convention(project.provider { null })
            applyLayout.convention(true)
            kotlinApiVersion.convention(KotlinVersion.KOTLIN_1_8)
            kotlinTestVersion.convention(KotlinVersion.DEFAULT)
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
                            compilations.named(KotlinCompilation.TEST_COMPILATION_NAME) {
                                project.logger.debug("Compilation ${project.name}:$name to be set to default Kotlin API: ${e.kotlinTestVersion.get()}")
                                compileTaskProvider.configure {
                                    compilerOptions {
                                        languageVersion = e.kotlinTestVersion
                                        apiVersion = e.kotlinTestVersion
                                    }
                                }
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
                                val isTest = name == KotlinCompilation.TEST_COMPILATION_NAME
                                compileTaskProvider.configure {
                                    compilerOptions {
                                        when {
                                            isTest -> {
                                                languageVersion = e.kotlinTestVersion
                                                apiVersion = e.kotlinTestVersion
                                            }

                                            isJvm -> apiVersion = e.kotlinApiVersion

                                            else -> apiVersion = KotlinVersion.DEFAULT
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

                is DokkaPlugin -> {
                    project.logger.info("Automatically configuring dokka from the project plugin for ${project.name}")
                    project.configureDokka(e.dokkaModuleName, e.dokkaVersion, e.dokkaOverrideTarget)
                }
            }
        }
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
    abstract val dokkaOverrideTarget: Property<String?>
    abstract val applyLayout: Property<Boolean>
    abstract val kotlinApiVersion: Property<KotlinVersion>
    abstract val kotlinTestVersion: Property<KotlinVersion>
}


private var _isSnapshot: Int = -1

val Project.isSnapshot: Boolean
    get() = when (_isSnapshot) {
        0 -> false
        1 -> true

        else -> {
            val r: Boolean = providers.gradleProperty("forceSnapshot")
                .map { it.lowercase() == "true" }
                .getOrElse(false) || "SNAPSHOT" in version.toString().uppercase(Locale.getDefault())

            r.also { _isSnapshot = if (it) 1 else 0 }
        }
    }

val TIMESTAMP_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm'Z'")
