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

package net.devrieze.gradle.ext

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.GradleDokkaSourceSetBuilder
import org.jetbrains.dokka.gradle.engine.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateModuleTask
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.net.URI
import java.net.URL

fun Project.configureDokka(
    myModuleName: Provider<String>,
    myModuleVersion: Provider<String>,
    dokkaOverrideTarget: Provider<String?>
) {
    logger.lifecycle("Configuring dokka for project($name)")
    tasks.configureEach {
        if (name.contains("dokka", ignoreCase = true)) {
            logger.lifecycle("Dokka task: ${name} (${this.javaClass.simpleName})")
        }
    }
    tasks.withType<AbstractDokkaLeafTask>().configureEach {
        logger.lifecycle("Configuring dokka task: ${name} (${this.javaClass.simpleName})")
        moduleName.convention(myModuleName)
        moduleVersion.convention(myModuleVersion)

        dokkaSourceSets.configureEach {
            this@configureDokka.configureDokkaSourceSet(this, dokkaOverrideTarget.getOrNull())
        }
    }
    extensions.configure<DokkaExtension>("dokka") {
        logger.lifecycle("Configuring dokka task: ${name} (${this.javaClass.simpleName})")
        moduleName.convention(myModuleName)
        moduleVersion.convention(myModuleVersion)

        dokkaSourceSets.configureEach {
            this@configureDokka.configureDokkaSourceSet(this, dokkaOverrideTarget.getOrNull())
        }
    }
}

private fun Project.url(value: String): URL = URI(value).toURL()

private fun Project.configureDokkaSourceSet(
    sourceSet: GradleDokkaSourceSetBuilder,
    dokkaOverrideTarget: String?
) {
    if (!sourceSet.suppress.get()) {
        logger.info("Configuring dokkaSourceSet:${project.name}:${sourceSet.name}")
        with(sourceSet) {
            if (name.startsWith("android")) {
                noAndroidSdkLink.set(false)
                noJdkLink.set(true)
            } else {
                noAndroidSdkLink.set(true)
                noJdkLink.set(false)
            }
            displayName.set(
                dokkaOverrideTarget ?: when (val dn = displayName.get()?.lowercase()) {
                    "jdk" -> "JVM"
                    "jvm",
                    "javashared",
                    "commonjvm",
                    "jvmcommon" -> "JVM"
                    "android" -> "Android"
                    "common" -> "Common"
                    "js" -> "JS"
                    "native" -> "Native"
                    "commondom" -> "Native"
                    "wasmcommon" -> "Wasm"
                    else -> dn
                }
            )
            logger.lifecycle("Configuring dokka on sourceSet: :${project.name}:$name = ${displayName.orNull}")

            includeNonPublic = false
            skipEmptyPackages = true
            skipDeprecated = true

            for (sourceRoot in sourceSet.sourceRoots) {
                val relativeRoot = sourceRoot.relativeTo(rootProject.projectDir)
                logger.lifecycle("Adding source link for root: $relativeRoot")
                sourceLink {
                    localDirectory = sourceRoot
                    val relURI = relativeRoot.toURI()
                    val absUrl = URI.create("https://github.com/pdvrieze/xmlutil/tree/master/").resolve(relURI)
                    remoteUrl = absUrl.toURL()
                }
            }

            externalDocumentationLink {
                url = url("https://kotlinlang.org/api/kotlinx.serialization/")
            }

            perPackageOption {
                matchingRegex.set(".*\\.(impl|internal)(|\\..*)")
                suppress.set(true)
            }
            logger.lifecycle("Dokka source set: '$name'")
            if ("Main" in name) {
                val readme = project.file(project.relativePath("src/README.md"))
                if (readme.exists() && readme.canRead()) {
                    includes.from(listOf(readme))
                    logger.lifecycle("Adding $readme to sourceSet :${project.name}:${name}(${displayName.orNull})")
                } else {
                    logger.warn("Missing $readme for project ${project.name}")
                }
            }
        }
    } else {
        logger.warn("Sourceset ${project.name}:${sourceSet.name} suppressed")
    }
}

private fun Project.configureDokkaSourceSet(
    sourceSet: DokkaSourceSetSpec,
    dokkaOverrideTarget: String?
) {
    if (!sourceSet.suppress.get()) {
        logger.info("Configuring dokkaSourceSet:${project.name}:${sourceSet.name}")
        with(sourceSet) {
            if (name.startsWith("android")) {
                enableAndroidDocumentationLink = true
                enableJdkDocumentationLink = false
            } else {
                enableAndroidDocumentationLink = false
                enableJdkDocumentationLink = true
            }
            displayName.set(
                dokkaOverrideTarget ?: when (val dn = displayName.get()?.lowercase()) {
                    "jdk" -> "JVM"
                    "jvm",
                    "javashared",
                    "commonjvm",
                    "jvmcommon" -> "JVM"
                    "android" -> "Android"
                    "common" -> "Common"
                    "js" -> "JS"
                    "native" -> "Native"
                    "commondom" -> "Native"
                    "wasmcommon" -> "Wasm"
                    else -> dn
                }
            )
            logger.lifecycle("Configuring dokka on sourceSet: :${project.name}:$name = ${displayName.orNull}")

            documentedVisibilities = setOf(VisibilityModifier.Public, VisibilityModifier.Protected)

            skipEmptyPackages = true
            skipDeprecated = true

            for (sourceRoot in sourceSet.sourceRoots) {
                val relativeRoot = sourceRoot.relativeTo(rootProject.projectDir)
                logger.lifecycle("Adding source link for root: $relativeRoot")
                sourceLink {
                    localDirectory = sourceRoot
                    val relURI = relativeRoot.toURI()
                    val absUrl = URI.create("https://github.com/pdvrieze/xmlutil/tree/master/").resolve(relURI)
                    remoteUrl = absUrl
                }
            }

            externalDocumentationLinks.apply {
                url("https://kotlinlang.org/api/kotlinx.serialization/")
            }

            perPackageOption {
                matchingRegex.set(".*\\.(impl|internal)(|\\..*)")
                suppress.set(true)
            }
            logger.lifecycle("Dokka source set: '$name'")
            if ("Main" in name) {
                val readme = project.file(project.relativePath("src/README.md"))
                if (readme.exists() && readme.canRead()) {
                    includes.from(listOf(readme))
                    logger.lifecycle("Adding $readme to sourceSet :${project.name}:${name}(${displayName.orNull})")
                } else {
                    logger.warn("Missing $readme for project ${project.name}")
                }
            }
        }
    } else {
        logger.warn("Sourceset ${project.name}:${sourceSet.name} suppressed")
    }
}
