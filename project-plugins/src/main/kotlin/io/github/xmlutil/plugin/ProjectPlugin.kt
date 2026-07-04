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
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.TEST_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import kotlin.jvm.optionals.getOrNull
import kotlin.time.ExperimentalTime

@Suppress("unused")
class ProjectPlugin @Inject constructor(
    private val softwareComponentFactory: SoftwareComponentFactory
) : Plugin<Project> {
    @OptIn(ExperimentalTime::class)
    override fun apply(project: Project) {
        project.logger.info("===================\nUsing ProjectPlugin\n===================")


        val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
        val xmlutilVersion = libs.findVersion("xmlutil").get().requiredVersion

        project.group = "io.github.pdvrieze.xmlutil"
        project.version = xmlutilVersion

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

        val projectConfiguration = project.extensions.create<ProjectConfigurationExtension>("config").apply {
            dokkaModuleName.convention(project.provider { project.name })
            dokkaVersion.convention(project.provider { project.version.toString() })
            dokkaOverrideTarget.convention(project.provider { null })
            applyLayout.convention(false)
            val apiVer = libs.findVersion("apiVersion").getOrNull()
                ?.run { requiredVersion.let { KotlinVersion.fromVersion(it) } }
                ?: KotlinVersion.KOTLIN_2_2
            kotlinApiVersion.convention(apiVer)
            kotlinTestVersion.convention(KotlinVersion.DEFAULT)
            createAndroidCompatComponent.convention(false)
            generateJavaModules.convention(true)
            allWarningsAsErrors.convention(true)
            generalJvmTarget.convention(JvmTarget.JVM_1_8)
            testJvmTarget.convention(JvmTarget.JVM_17)
            optIns.convention(
                listOf(
                    "nl.adaptivity.xmlutil.ExperimentalXmlUtilApi",
                    "nl.adaptivity.xmlutil.XmlUtilInternal",
                    "nl.adaptivity.xmlutil.XmlUtilDeprecatedInternal",
                )
            )
        }

        project.afterEvaluate {

            if(projectConfiguration.generateJavaModules.get()) {
                project.configureJava9ModuleInfo()
            }

            if (projectConfiguration.createAndroidCompatComponent.get()) {
                val configurations = project.configurations

                project.logger.warn("Creating compatible component")

                @Suppress("UnstableApiUsage")
                val androidRuntime = configurations.dependencyScope("androidRuntime") {
                    dependencies.add(project.dependencyFactory.create("io.github.pdvrieze.xmlutil:${project.name}:${project.version}"))
                }

                @Suppress("UnstableApiUsage")
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
                    logger.debug("Add variant to runtime scope")
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


        project.plugins.configureEach {
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
                    project.afterEvaluate {
                        project.extensions.configure<KotlinJvmProjectExtension> {
                            compilerOptions {
                                apiVersion = projectConfiguration.kotlinApiVersion
                                project.logger.info("Setting kotlin compilation options for JVM project ${project.name}")
                                configureCompilerOptions(project, "project ${project.name}", projectConfiguration)
                            }

                            target {
                                attributes {
                                    attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.envJvm)
                                    attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
                                }
                                compilations.named(TEST_COMPILATION_NAME) {
                                    project.logger.debug(
                                        "Compilation {}:{} to be set to default Kotlin API: {}",
                                        project.name,
                                        name,
                                        projectConfiguration.kotlinTestVersion.get()
                                    )
                                    compileTaskProvider.configure {
                                        compilerOptions {
                                            (this as? KotlinJvmCompilerOptions)?.run {
                                                jvmTarget = JvmTarget.DEFAULT
                                            }
                                            languageVersion = projectConfiguration.kotlinTestVersion
                                            apiVersion = projectConfiguration.kotlinTestVersion
                                        }
                                    }
                                }
                                mavenPublication {
                                    version = xmlutilVersion
                                    project.logger.info("Setting maven publication ($artifactId) version to $xmlutilVersion")
                                }
                            }
                        }

                    }

                }

                is KotlinMultiplatformPluginWrapper -> project.afterEvaluate {
                    project.extensions.configure<KotlinMultiplatformExtension> {
                        if(projectConfiguration.applyLayout.get()) applyDefaultXmlUtilHierarchyTemplate()
                        compilerOptions {
                            project.logger.info("Setting kotlin compilation options for multiplatform project ${project.name}")
                            configureCommonCompilerOptions(project, "project ${project.name}", projectConfiguration)
                        }
                        targets.configureEach {
                            val isJvm = this is KotlinJvmTarget
                            this.compilations.configureEach {
                                val isTest = name == TEST_COMPILATION_NAME
                                compileTaskProvider.configure {
                                    compilerOptions {
                                        when {
                                            isTest -> {
                                                languageVersion = projectConfiguration.kotlinTestVersion
                                                apiVersion = projectConfiguration.kotlinTestVersion
                                            }

                                            isJvm -> apiVersion = projectConfiguration.kotlinApiVersion

                                            else -> apiVersion = KotlinVersion.DEFAULT
                                        }
                                    }
                                }
                            }
                            mavenPublication {
                                version = xmlutilVersion
                                project.logger.info("Setting multiplatform maven publication ($artifactId) version to $xmlutilVersion")
                            }
                            if (this is KotlinMetadataTarget && projectConfiguration.allWarningsAsErrors.get()) {
                                project.logger.info("allWarningsAsErrors is disabled for target $name")
                                compilerOptions {
                                    allWarningsAsErrors = false
                                }
                            }
                        }


                        targets.withType<KotlinJvmTarget> {
                            compilations.configureEach {
                                val target = when {
                                    name == TEST_COMPILATION_NAME -> projectConfiguration.testJvmTarget
                                    else -> projectConfiguration.generalJvmTarget
                                }
                                compileTaskProvider.configure {
                                    compilerOptions {
                                        project.logger.info("Setting kotlin compilation options JVM compile task provider ${project.name}")

                                        configureJvmCompilerOptions(project, "${project.name}:$name", target)
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
                    project.configureDokka(projectConfiguration.dokkaModuleName, projectConfiguration.dokkaVersion, projectConfiguration.dokkaOverrideTarget)
                }
            }
        }
        project.afterEvaluate {
            for (c in project.components) {
                project.logger.debug("Found component: ${c.name}")
            }
        }
    }

    private fun KotlinCommonCompilerOptions.configureCompilerOptions(
        project: Project,
        name: String,
        projectConfiguration: ProjectConfigurationExtension,
    ) {
        configureCommonCompilerOptions(project, name, projectConfiguration)
        if (this is KotlinJvmCompilerOptions) {
            configureJvmCompilerOptions(project, name, projectConfiguration.generalJvmTarget)
        }
    }

    private fun KotlinCommonCompilerOptions.configureCommonCompilerOptions(project: Project, name: String, projectConfiguration: ProjectConfigurationExtension) {
        progressiveMode = true
        languageVersion = KotlinVersion.DEFAULT
        allWarningsAsErrors = projectConfiguration.allWarningsAsErrors
        val optIns = projectConfiguration.optIns.get()
        when (optIns.size) {
            0 -> {
                optIn.set(emptyList()) // just reset it now
                project.logger.info("No opt-ins specified for project ${project.name}/$name. Current opt-ins: ${optIn.get().joinToString()}")
            }
            else -> for (it in optIns) {
                project.logger.info("Enabling opt-in: $it for project ${project.name}/$name")
                optIn.add(it)
            }
        }
        freeCompilerArgs.add("-Xreturn-value-checker=full")
    }

    private fun KotlinJvmCompilerOptions.configureJvmCompilerOptions(
        project: Project,
        name: String,
        target: Property<JvmTarget>
    ) {
        project.logger.info("Setting jvm compilation options for $name")
        this.jvmTarget = target
        this.jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
    }

}

abstract class ProjectConfigurationExtension {
    abstract val generalJvmTarget: Property<JvmTarget>
    abstract val testJvmTarget: Property<JvmTarget>
    abstract val dokkaModuleName: Property<String>
    abstract val dokkaVersion: Property<String>
    abstract val dokkaOverrideTarget: Property<String>
    abstract val applyLayout: Property<Boolean>
    abstract val kotlinApiVersion: Property<KotlinVersion>
    abstract val kotlinTestVersion: Property<KotlinVersion>
    abstract val createAndroidCompatComponent: Property<Boolean>
    abstract val generateJavaModules: Property<Boolean>
    abstract val allWarningsAsErrors: Property<Boolean>
    abstract val optIns: ListProperty<String>
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
