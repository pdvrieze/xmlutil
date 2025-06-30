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

package io.github.xmlutil.plugin

import net.devrieze.gradle.ext.applyDefaultXmlUtilHierarchyTemplate
import net.devrieze.gradle.ext.configureDokka
import net.devrieze.gradle.ext.envAndroid
import net.devrieze.gradle.ext.envJvm
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import javax.inject.Inject
import kotlin.jvm.optionals.getOrNull

class ProjectPlugin @Inject constructor(
    private val softwareComponentFactory: SoftwareComponentFactory
): Plugin<Project> {
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
            dokkaOverrideTarget.convention(project.provider { null })
            applyLayout.convention(true)
            val apiVer = libs.findVersion("apiVersion").getOrNull()
                ?.run { requiredVersion.let { KotlinVersion.fromVersion(it) } }
                ?: KotlinVersion.KOTLIN_2_0
            kotlinApiVersion.convention(apiVer)
            kotlinTestVersion.convention(KotlinVersion.DEFAULT)
            createAndroidCompatComponent.convention(false)
            generateJavaModules.convention(true)
        }

        project.afterEvaluate {

            if(e.generateJavaModules.get()) {
                project.configureJava9ModuleInfo()
            }

            if (e.createAndroidCompatComponent.get()) {
                val configurations = project.configurations

                project.logger.warn("Creating compatible component")

                val androidRuntime = configurations.dependencyScope("androidRuntime") {
                    dependencies.add(project.dependencyFactory.create("io.github.pdvrieze.xmlutil:${project.name}:${project.version}"))
                }
                val androidRuntimeElements = configurations.consumable("androidRuntimeElements") {
                    extendsFrom(androidRuntime.get())
                    attributes {
                        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_API))
                        // see whether this should be library
                        attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
                        attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.envAndroid)
                        attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
                    }
                }

                val component = softwareComponentFactory.adhoc("androidComponent")
                project.components.add(component)

                component.addVariantsFromConfiguration(androidRuntimeElements.get()) {
                    logger.lifecycle("Add variant to runtime scope")
                    mapToMavenScope("runtime")
                }


                project.extensions.configure<PublishingExtension> {
                    publications {
                        create<MavenPublication>("android") {
                            artifactId = "${project.name}-android"
                            from(component)

                            // important so publishing does not try to resolve this when consuming the project
                            (this as? DefaultMavenPublication)?.let { it.isAlias = true }
                        }
                    }
                }
            }

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
                                "jvmCommon" -> {} // no attributes needed
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
        project.afterEvaluate {
            for (c in project.components) {
                project.logger.warn("Found component: ${c.name}")
            }
        }
    }

    private fun KotlinCommonCompilerOptions.configureCompilerOptions(project: Project, name: String) {
        progressiveMode = true
        languageVersion = KotlinVersion.DEFAULT
        configureOptins()
        if (this is KotlinJvmCompilerOptions) {
            project.logger.info("Setting common compilation options for $name")
            jvmTarget = JvmTarget.JVM_1_8
            jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
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
    abstract val createAndroidCompatComponent: Property<Boolean>
    abstract val generateJavaModules: Property<Boolean>
}
