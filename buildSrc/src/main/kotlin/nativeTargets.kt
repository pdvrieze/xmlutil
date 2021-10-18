/*
 * Copyright (c) 2021.
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

package net.devrieze.gradle.ext

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetPreset
import org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeTargetPreset

enum class NativeState {
    ALL, HOST, SINGLE, DISABLED
}

internal fun getHostName(): String {
    val target = System.getProperty("os.name")
    return when {
        target == "Linux" -> "linux"
        target.startsWith("Windows") -> "windows"
        target.startsWith("Mac") -> "macos"
        else -> "unknown"
    }
}

private fun NamedDomainObjectCollection<KotlinTargetPreset<*>>.nativePreset(name: String): AbstractKotlinNativeTargetPreset<*> {
    return getByName<AbstractKotlinNativeTargetPreset<*>>(name)
}

fun Project.addNativeTargets() {
    val ideaActive = System.getProperty("idea.active") == "true"
    val nativeState = when(property("native.deploy")?.toString()?.toLowerCase()) {
        "all", "true" -> NativeState.ALL
        "host" -> NativeState.HOST
        "disabled" -> NativeState.DISABLED
        else -> NativeState.SINGLE
    }
    val singleTargetMode = ideaActive || nativeState == NativeState.SINGLE

    val ext = extensions.getByName<ExtraPropertiesExtension>("ext")
    val manager = HostManager()//ext["hostManager"] as HostManager
    val kotlin = extensions.getByName<KotlinMultiplatformExtension>("kotlin")

    val presets: NamedDomainObjectCollection<KotlinTargetPreset<*>> = kotlin.presets
    val linuxEnabled = manager.isEnabled(presets.nativePreset("linuxX64").konanTarget)
    val macosEnabled = manager.isEnabled(presets.nativePreset("macosX64").konanTarget)
    val winEnabled = manager.isEnabled(presets.nativePreset("mingwX64").konanTarget)

    ext["ideaPreset"] = when {
        winEnabled -> presets.nativePreset("mingwX64")
        macosEnabled -> presets.nativePreset("macosX64")
        else -> presets.nativePreset("linuxX64")
    }

    val nativeMainSets = mutableListOf<KotlinSourceSet>()
    val nativeTestSets = mutableListOf<KotlinSourceSet>()

    fun KotlinNativeTarget.addSourceSets() {
        nativeMainSets.add(compilations.getByName("main").kotlinSourceSets.first())
        nativeTestSets.add(compilations.getByName("test").kotlinSourceSets.first())
    }

    fun KotlinTargetContainerWithPresetFunctions.addTarget(targetName: String) {
        kotlin.targetFromPreset(presets.getByName<AbstractKotlinNativeTargetPreset<*>>(targetName)) {
            addSourceSets()
        }
    }

    if (nativeState != NativeState.DISABLED) {
        with(kotlin) {
            targets {
                if (singleTargetMode) {
                    kotlin.targetFromPreset(ext["ideaPreset"] as AbstractKotlinNativeTargetPreset<*>, "native")
                } else {
                    linuxX64 { addSourceSets() }
                    linuxArm32Hfp { addSourceSets() }
                    linuxArm64 { addSourceSets() }

                    macosX64 { addSourceSets() }
                    iosArm64 { addSourceSets() }
                    iosArm32 { addSourceSets() }
                    iosX64 { addSourceSets() }

                    watchosX86 { addSourceSets() }
                    watchosX64 { addSourceSets() }
                    watchosArm32 { addSourceSets() }
                    watchosArm64 { addSourceSets() }

                    tvosArm64 { addSourceSets() }
                    tvosX64 { addSourceSets() }

                    addTarget("iosSimulatorArm64")
                    addTarget("watchosSimulatorArm64")
                    addTarget("tvosSimulatorArm64")
                    addTarget("macosArm64")

                    mingwX64 { addSourceSets() }
                    mingwX86 { addSourceSets() }
                }
            }

            sourceSets {
                val commonMain = getByName("commonMain")
                val commonTest = getByName("commonTest")
                if(singleTargetMode) {
                    getByName("nativeMain") { dependsOn(commonMain) }
                    getByName("nativeTest") { dependsOn(commonTest) }
                } else {
                    val nativeMain = maybeCreate("nativeMain").apply { dependsOn(commonMain) }
                    val nativeTest = maybeCreate("nativeTest").apply { dependsOn(commonTest) }

                    configure(nativeMainSets) { dependsOn(nativeMain) }
                    configure(nativeTestSets) { dependsOn(nativeTest) }
                }
            }
        }

    }

}

private fun KotlinMultiplatformExtension.targets(configure: Action<Any>): Unit =
    (this as ExtensionAware).extensions.configure("targets", configure)

private fun KotlinMultiplatformExtension.sourceSets(configure: Action<org.gradle.api.NamedDomainObjectContainer<KotlinSourceSet>>): Unit =
    (this as ExtensionAware).extensions.configure("sourceSets", configure)
