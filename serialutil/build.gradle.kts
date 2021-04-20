/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

@file:Suppress("PropertyName")

import net.devrieze.gradle.ext.configureDokka
import net.devrieze.gradle.ext.doPublish
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    kotlin("multiplatform")
    id("kotlinx-serialization")
    id("maven-publish")
    id("signing")
    idea
    id("org.jetbrains.dokka")
}

val xmlutil_version: String by project
val xmlutil_versiondesc: String by project

base {
    archivesBaseName = "serialutil"
    version = xmlutil_version
}

val serializationVersion: String by project

val kotlin_version: String by project

val androidAttribute = Attribute.of("net.devrieze.android", Boolean::class.javaObjectType)

val moduleName = "net.devrieze.serialutil"

kotlin {
    targets {
        jvm {
            attributes {
                attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
                attribute(androidAttribute, false)
            }
            compilations.all {
                tasks.named<KotlinCompile>(compileKotlinTaskName) {
                    kotlinOptions {
                        jvmTarget = "1.8"
                    }
                }
                tasks.named<Jar>("jvmJar") {
                    manifest {
                        attributes("Automatic-Module-Name" to moduleName)
                    }
                }
            }
        }
        jvm("android") {
            attributes {
                attribute(androidAttribute, true)
                attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 6)
                attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
            }
            compilations.all {
                kotlinOptions {
                    jvmTarget = "1.6"
                }
            }
        }
        js(BOTH) {
            browser()
            nodejs()
            compilations.all {
                kotlinOptions {
                    sourceMap = true
                    sourceMapEmbedSources = "always"
                    suppressWarnings = false
                    verbose = true
                    metaInfo = true
                    moduleKind = "umd"
                    main = "call"
                }
            }
        }

    }
    targets.forEach { target ->
        target.compilations.all {
            kotlinOptions {
                languageVersion = "1.4"
                apiVersion = "1.4"
                freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
            }
        }
/*
        val javadocJar = tasks.create<Jar>(target.targetName+"JavadocJar") {
            archiveClassifier.set("javadoc")

        }
        val sourcesJar = tasks.create<Jar>(target.targetName+"SourcesJar") {
            archiveClassifier.set("sources")
        }
*/
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
            }
        }
        val javaShared by creating {
            dependsOn(commonMain)
        }

        val jvmMain by getting {
            dependsOn(javaShared)
        }

        val androidMain by getting {
            dependsOn(javaShared)
        }

    }

}

components.forEach { component ->

    logger.lifecycle("Found component ${component.name} of type: ${component.javaClass} (isAdhoc:${component is AdhocComponentWithVariants})")
}

doPublish()

configureDokka(myModuleVersion = xmlutil_version)

idea {
    module {
        name = "xmlutil-serialutil"
    }
}
