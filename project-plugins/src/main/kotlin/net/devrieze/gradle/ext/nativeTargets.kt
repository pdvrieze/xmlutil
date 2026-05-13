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

package net.devrieze.gradle.ext

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.konan.target.HostManager

enum class Host {
    Windows,
    Macos,
    Linux
}

enum class NativeState {
    ALL{
        override val hasWasm: Boolean get() = true
    },
    SINGLE {
        override val hasWasm: Boolean get() = true
    },
    HOST {
        override val hasWasm: Boolean get() = true
    },
    DISABLED;

    open val hasWasm: Boolean get() = false
}

private typealias TargetFun = KotlinMultiplatformExtension.() -> Unit

@OptIn(ExperimentalKotlinGradlePluginApi::class)
fun KotlinMultiplatformExtension.applyDefaultXmlUtilHierarchyTemplate() {
    applyHierarchyTemplate(defaultXmlUtilHierarchyTemplate)

}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
private val defaultXmlUtilHierarchyTemplate = KotlinHierarchyTemplate {
    withSourceSetTree(KotlinSourceSetTree.main, KotlinSourceSetTree.test)

    common {
        withCompilations { c -> c.target.platformType !in arrayOf(KotlinPlatformType.jvm, KotlinPlatformType.wasm) }

        group("javaShared") {
            withCompilations { c ->
                c.target.platformType == KotlinPlatformType.jvm && "jvm" !in c.target.name
            }

            group("commonJvm") {
                withCompilations { c ->
                    c.target.platformType == KotlinPlatformType.jvm && "jvm" in c.target.name
                }
            }
        }

        group("commonDom") {

            group("wasmCommon") {
                withWasmJs()
                withWasmWasi()
            }

            group("native") {
                group("apple") {
                    withApple()

                    group("ios") {
                        withIos()
                    }

                    group("tvos") {
                        withTvos()
                    }

                    group("watchos") {
                        withWatchos()
                    }

                    group("macos") {
                        withMacos()
                    }
                }

                group("linux") {
                    withLinux()
                }

                group("mingw") {
                    withMingw()
                }

                group("androidNative") {
                    withAndroidNative()
                }

            }
        }
    }
}

val Project.nativeState: NativeState
    get() = rootProject.extraProperties["nativeTargets"] as NativeState

fun Project.isKlibValidationEnabled(): Boolean = when {
    rootProject.extraProperties.has("nativeTargets") -> nativeState == NativeState.ALL

    else -> property("native.deploy")?.toString()?.lowercase() == "all"
}

fun Project.addNativeTargets(includeWasm: Boolean = true, includeWasi: Boolean = true) {
    val ideaActive = System.getProperty("idea.active") == "true"
    val nativeState = when(property("native.deploy")?.toString()?.lowercase()) {
        "all", "true" -> NativeState.ALL
        "host" -> NativeState.HOST
        "hostWasm" -> NativeState.HOST
        "disabled" -> NativeState.DISABLED
        "single" -> NativeState.SINGLE
        else if gradle.startParameter.taskRequests.any { it.args.any { "updateKotlinAbi" in it} } -> {
            logger.lifecycle("No native.deploy property set, and abi update task found.\n" +
                    "  -- Defaulting to all mode")
            NativeState.ALL
        }
        else -> {
            logger.lifecycle("set the native.deploy=[all|host|hostWasm|disabled|single] property to specify the native mode.\n" +
                    "  -- Defaulting to single mode")
            NativeState.SINGLE
        }
    }
    rootProject.extraProperties.set("nativeTargets", nativeState)

    if (nativeState == NativeState.DISABLED) return

    val singleTargetMode = /*ideaActive || */nativeState == NativeState.SINGLE

    val ext = extensions.getByName<ExtraPropertiesExtension>("ext")
    val kotlin = extensions.getByName<KotlinMultiplatformExtension>("kotlin")

    val host = when(HostManager.hostOs()) {
        "windows" -> Host.Windows
        "macos" -> Host.Macos
        else -> Host.Linux
    }

    val manager = HostManager()//ext["hostManager"] as HostManager
    val hostTarget = manager.targetByName("host")

    ext["ideaPreset"] = when (host) {
        Host.Windows -> when (HostManager.hostArchOrNull()) {
            "x86_64" -> fun KotlinMultiplatformExtension.() { mingwX64() }
            "aarch64" -> fun KotlinMultiplatformExtension.() { /* No-op as not supported as native target yet */ }
            else -> return // unknown/unsupported target
        }
        Host.Macos -> when (HostManager.hostArchOrNull()) {
            "x86_64" -> fun KotlinMultiplatformExtension.() { macosX64() }
            "aarch64" -> fun KotlinMultiplatformExtension.() { macosArm64() }
            else -> fun KotlinMultiplatformExtension.() { /** No op, not supported */ }
        }
        Host.Linux -> when (HostManager.hostArchOrNull()) {
            "x86_64" -> fun KotlinMultiplatformExtension.() { linuxX64() }
            "aarch64" -> fun KotlinMultiplatformExtension.() { linuxArm64() }
            else -> fun KotlinMultiplatformExtension.() { /** No op, unsupported target */ }
        }
    }

    with(kotlin) {
        if (nativeState.hasWasm) {
            if (includeWasm) {
                logger.lifecycle("Adding WASM support")
                wasmJs() {
                    nodejs()
                    browser {
                        testTask {
                            isEnabled = ! System.getenv().containsKey("GITHUB_ACTION")
                        }
                    }
                }
            }
            if (includeWasi) {
                logger.lifecycle("Adding WASI support")
                wasmWasi {
                    nodejs()
                }
            }
        }

        if (singleTargetMode) {
            logger.lifecycle("Single target mode: $host (${HostManager.hostArchOrNull()})")
            @Suppress("UNCHECKED_CAST") val targetFun = ext["ideaPreset"] as TargetFun
            targetFun()
        } else {
            if (nativeState != NativeState.HOST || host == Host.Linux) {
                logger.lifecycle("Adding Linux targets")
                linuxX64()
                linuxArm64()
                @Suppress("DEPRECATION")
                linuxArm32Hfp()
            }

            if (nativeState != NativeState.HOST || host == Host.Macos) {
                logger.lifecycle("Adding Mac(ish) targets")
                macosX64()
                macosArm64()
                iosArm64()
                iosSimulatorArm64()
                iosX64()

                watchosDeviceArm64()
                watchosSimulatorArm64()
                watchosX64()
                watchosArm32()
                watchosArm64()

                tvosSimulatorArm64()
                tvosArm64()
                tvosX64()
            }

            if (nativeState != NativeState.HOST || host == Host.Windows) {
                logger.lifecycle("Adding Windows x64 target")
                mingwX64()
            }

            if (nativeState != NativeState.HOST) {
                logger.lifecycle("Adding Android native targets")
                androidNativeArm32()
                androidNativeArm64()
                androidNativeX86()
                androidNativeX64()
            }
        }

        project.logger.debug("Registering :${project.name}:nativeTest")
        project.tasks.register("nativeTest") {
            group = "verification"
            val testTasks = tasks.withType<KotlinNativeTest>().filter {
                val upperTarget = it.targetName ?: "UNSUPPORTED_TARGET"
                it is KotlinNativeHostTest
                        upperTarget.contains(host.name, true) &&
                        HostManager.hostArchOrNull().let { a -> a != null && upperTarget.contains(a, true) }
            }
            project.logger.debug("Configuring $path with host/target: ${host.name}/${HostManager.hostArchOrNull()} to depend on ${testTasks.joinToString { it.path}}")
            dependsOn(testTasks)
        }
    }
}

private fun KotlinMultiplatformExtension.targets(configure: Action<Any>): Unit =
    (this as ExtensionAware).extensions.configure("targets", configure)

private fun KotlinMultiplatformExtension.sourceSets(configure: Action<org.gradle.api.NamedDomainObjectContainer<KotlinSourceSet>>): Unit =
    (this as ExtensionAware).extensions.configure("sourceSets", configure)

val Project.isWasmSupported: Boolean get() = true
