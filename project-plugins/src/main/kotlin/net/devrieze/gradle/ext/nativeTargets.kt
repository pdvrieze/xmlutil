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

import io.github.xmlutil.plugin.isSnapshot
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyTemplate
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
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
        withCompilations { true }

        group("nativeOrWasm") {
            withCompilations { it.platformType in arrayOf(KotlinPlatformType.native,KotlinPlatformType.wasm) }

            group("wasmCommon") {
                withWasmJs()
                withWasmWasi()
            }

            group("native") {
                withNative()

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

private lateinit var _nativeState: Provider<NativeState>

internal fun Project.initNativeState() {
    _nativeState = providers.gradleProperty("native.deploy").map {
        when(it.lowercase()) {
            "all", "true" -> NativeState.ALL
            "host" -> NativeState.HOST
            "hostWasm" -> NativeState.HOST
            "disabled" -> NativeState.DISABLED
            "single" -> NativeState.SINGLE
            else if gradle.startParameter.taskRequests.any { req ->
                req.args.any { arg ->
                    listOf( "checkKotlinAbi", "updateKotlinAbi").any { it in arg } || arg.endsWith("check")
                }
            } -> NativeState.ALL
            else -> {
                NativeState.SINGLE
            }
        }
    }.orElse(NativeState.SINGLE)
}

val Project.nativeState: NativeState
    get() = _nativeState.get()

fun Project.isKlibValidationEnabled(): Boolean = nativeState == NativeState.ALL

@Suppress("DEPRECATION")
private fun KotlinMultiplatformExtension.ideaPreset(host: Host) {
    when (host) {
        Host.Windows -> when (HostManager.hostArchOrNull()) {
            "x86_64" -> mingwX64()
            "aarch64" -> Unit /* No-op as not supported as native target yet */
            else -> Unit /* No op, not supported */
        }

        Host.Macos -> when (HostManager.hostArchOrNull()) {
            "x86_64" -> macosX64()
            "aarch64" -> macosArm64()
            else -> Unit /* No op, not supported */
        }

        Host.Linux -> when (HostManager.hostArchOrNull()) {
            "x86_64" -> linuxX64()
            "aarch64" -> linuxArm64()
            else -> Unit /* No op, unsupported target */
        }
    }
}

@OptIn(ExperimentalWasmDsl::class)
fun Project.addNativeTargets(includeWasm: Boolean = true, includeWasi: Boolean = true) {
    if (nativeState == NativeState.DISABLED) return

    val singleTargetMode = /*ideaActive || */nativeState == NativeState.SINGLE

    val ext = extensions.getByName<ExtraPropertiesExtension>("ext")
    val kotlin = extensions.getByName<KotlinMultiplatformExtension>("kotlin")

    val host = when(HostManager.hostOs()) {
        "windows" -> Host.Windows
        "macos" -> Host.Macos
        else -> Host.Linux
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
            ideaPreset(host)
        } else {
            val ignoreDeprecated = isSnapshot && ! System.getenv("JITPACK").equals("true", true)
            if (nativeState != NativeState.HOST || host == Host.Linux) {
                logger.lifecycle("Adding Linux targets")
                linuxX64()
                linuxArm64()
                @Suppress("DEPRECATION")
                if (!ignoreDeprecated) linuxArm32Hfp()
            }

            @Suppress("DEPRECATION")
            if (nativeState != NativeState.HOST || host == Host.Macos) {
                logger.lifecycle("Adding Mac(ish) targets")
                if (!ignoreDeprecated) macosX64()
                macosArm64()
                iosArm64()
                iosSimulatorArm64()
                iosX64()

                watchosDeviceArm64()
                watchosSimulatorArm64()
                if (!ignoreDeprecated) watchosX64()
                watchosArm32()
                watchosArm64()

                tvosSimulatorArm64()
                tvosArm64()
                if (!ignoreDeprecated) tvosX64()
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

