/*
 * Copyright (c) 2025.
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

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinApiPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.io.File

internal val KotlinProjectExtension.targets: Iterable<KotlinTarget>
    get() = when(this) {
        is KotlinSingleTargetExtension<*> -> listOf(this.target)
        is KotlinMultiplatformExtension -> targets
        else -> error("Unexpected 'kotlin' extension $this")
    }

fun Project.configureJava9ModuleInfo(multiRelease: Boolean = true) {
    val disableJPMS = this.rootProject.extra.has("disableJPMS")
    val ideaActive = System.getProperty("idea.active") == "true"
//    if (disableJPMS || ideaActive) return
    val kotlin = extensions.findByType<KotlinProjectExtension>() ?: return
    val jvmTargets = kotlin.targets.filter { it is KotlinJvmTarget || it is KotlinWithJavaTarget<*, *> }

    for(target in jvmTargets) {
        logger.lifecycle("Configuring java 9 module support for $name(target = ${target.name})")
        val artifactTask = tasks.named(target.artifactsTaskName, Jar::class.java) {
            if (multiRelease) {
                manifest { attributes("Multi-Release" to true) }
            }
        }

        for (compilation in target.compilations) {
            if (compilation.name == "test") continue

            @Suppress("UNCHECKED_CAST")
            val compileKotlinTask = compilation.compileTaskProvider as TaskProvider<KotlinCompile>
            val defaultSourceSet = compilation.defaultSourceSet

            val sourceSetName = "${defaultSourceSet.name}Module"

            kotlin.sourceSets.create(sourceSetName) {
                logger.info("Registering source set for module info: $sourceSetName in compilation: ${compilation.name}")
                val sourceFile = this.kotlin.find { it.name == "module-info.java" }
                logger.debug("  Found module info: $sourceFile")
                val targetDirectory = compileKotlinTask.flatMap { task ->
                    task.destinationDirectory.map { it.dir("../${it.asFile.name}Module") }
                }

                if (sourceFile != null) {
                    val verifyModuleTask = registerVerifyModuleTask(compileKotlinTask, sourceFile)
                    tasks.named("check") {
                        dependsOn(verifyModuleTask)
                    }

                    val compileModuleTask = registerCompileModuleTask(compileKotlinTask, sourceFile, targetDirectory)

                    artifactTask.configure {
                        from(compileModuleTask.map { it.destinationDirectory }) {
                            if (multiRelease) {
                                into("META-INF/versions/9")
                            }
                        }
                    }
                } else {
                    logger.lifecycle("No module-info.java file found in ${this.kotlin.srcDirs}, can't configure compilation of module-info!")
                }
            }

        }
    }
}

private fun Project.registerVerifyModuleTask(
    compileTask: TaskProvider<KotlinCompile>,
    sourceFile: File
): TaskProvider<out KotlinJvmCompile> {
    apply<KotlinApiPlugin>()

    @Suppress("DEPRECATION")
    val verifyModuleTaskName = "verify${compileTask.name.removePrefix("compile").capitalize()}Module"
    // work-around for https://youtrack.jetbrains.com/issue/KT-60542
    val kotlinApiPlugin = plugins.getPlugin(KotlinApiPlugin::class)
    val verifyModuleTask = kotlinApiPlugin.registerKotlinJvmCompileTask(
        verifyModuleTaskName,
        compilerOptions = compileTask.get().compilerOptions,
        explicitApiMode = provider { ExplicitApiMode.Disabled },
    )
    verifyModuleTask {
        group = VERIFICATION_GROUP
        description = "Verify Kotlin sources for JPMS problems"
        libraries.from(compileTask.map { it.libraries })
        source(compileTask.map { it.sources })
        source(compileTask.map { it.javaSources })
        source(sourceFile)
        destinationDirectory.set(temporaryDir)
        multiPlatformEnabled.set(compileTask.get().multiPlatformEnabled)

        //extensions.getByName("javaToolchains") as org.gradle.jvm.toolchain.JavaToolchainService
        val javaToolChains = project.extensions.findByType<JavaToolchainService>()!!

        kotlinJavaToolchain.toolchain.use(javaToolChains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(11))
        })

        pluginClasspath.from(compileTask.map { it.pluginClasspath })

        compilerOptions {

            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs.addAll(
                listOf("-Xjdk-release=11",  "-Xsuppress-version-warnings", "-Xexpect-actual-classes")
            )
        }
        // work-around for https://youtrack.jetbrains.com/issue/KT-60583
        inputs.files(
            libraries.asFileTree.elements.map { libs ->
                libs
                    .filter { it.asFile.exists() }
                    .map {
                        zipTree(it.asFile).filter { it.name == "module-info.class" }
                    }
            }
        ).withPropertyName("moduleInfosOfLibraries")
        this as KotlinCompile

        @OptIn(InternalKotlinGradlePluginApi::class)
        multiplatformStructure.refinesEdges.set(compileTask.flatMap { it.multiplatformStructure.refinesEdges })
        @OptIn(InternalKotlinGradlePluginApi::class)
        multiplatformStructure.fragments.set(compileTask.flatMap { it.multiplatformStructure.fragments })
        // part of work-around for https://youtrack.jetbrains.com/issue/KT-60541
        // and work-around for https://youtrack.jetbrains.com/issue/KT-60582
        incremental = false
    }
    return verifyModuleTask
}

private fun Project.registerCompileModuleTask(
    compileTask: TaskProvider<KotlinCompile>,
    sourceFile: File,
    targetDirectory: Provider<out Directory>
): TaskProvider<JavaCompile> {
    return tasks.register("${compileTask.name}Module", JavaCompile::class) {
        // Configure the module compile task.
        source(sourceFile)
        classpath = files()
        destinationDirectory.set(targetDirectory)
        // use a Java 11 toolchain with release 9 option
        // because for some OS / architecture combinations
        // there are no Java 9 builds available
        javaCompiler.set(
            this@registerCompileModuleTask.the<JavaToolchainService>().compilerFor {
                languageVersion.set(JavaLanguageVersion.of(11))
            }
        )
        options.release.set(9)

        options.compilerArgumentProviders.add(object : CommandLineArgumentProvider {
            @get:CompileClasspath
            val compileClasspath = objects.fileCollection().from(
                compileTask.map { it.libraries }
            )

            @get:CompileClasspath
            val compiledClasses = compileTask.flatMap { it.destinationDirectory }

            @get:Input
            val moduleName = sourceFile
                .readLines()
                .single { it.contains("module ") }
                .substringAfter("module ")
                .substringBefore(' ')
                .trim()

            override fun asArguments() = mutableListOf(
                // Provide the module path to the compiler instead of using a classpath.
                // The module path should be the same as the classpath of the compiler.
                "--module-path",
                compileClasspath.asPath,
                "--patch-module",
                "$moduleName=${compiledClasses.get()}",
                "-Xlint:-requires-transitive-automatic"
            )
        })
    }
}

