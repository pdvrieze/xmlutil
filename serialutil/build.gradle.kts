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

import com.jfrog.bintray.gradle.BintrayExtension
import net.devrieze.gradle.ext.fixBintrayModuleUpload
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    kotlin("multiplatform")
    id("kotlinx-serialization")
    id("maven-publish")
    id("com.jfrog.bintray")
    idea
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
                        freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
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
                tasks.getByName<KotlinCompile>(compileKotlinTaskName).kotlinOptions {
                    jvmTarget = "1.6"
                    freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
                }
            }
        }
        js {
            browser()
            nodejs()
            compilations.all {
                tasks.getByName<KotlinJsCompile>(compileKotlinTaskName).kotlinOptions {
                    sourceMap = true
                    sourceMapEmbedSources = "always"
                    suppressWarnings = false
                    verbose = true
                    metaInfo = true
                    moduleKind = "umd"
                    main = "call"
                    freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
                }
            }
        }

    }
    targets.forEach { target ->
        target.mavenPublication {
            groupId = "net.devrieze"
            artifactId = "serialutil-${target.targetName}"
            version = xmlutil_version
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationVersion")
            }
        }
        val javaShared by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(kotlin("stdlib-jdk7"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
            }
        }
        val jvmMain by getting {
            dependsOn(javaShared)
            dependencies {
                implementation(kotlin("stdlib-jdk7"))
            }
        }
        val androidMain by getting {
            dependsOn(javaShared)
        }
        val jsMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlin_version")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serializationVersion")
            }
        }

    }

}

components.forEach { component ->

    logger.lifecycle("Found component ${component.name} of type: ${component.javaClass} (isAdhoc:${component is AdhocComponentWithVariants})")
}

repositories {
    jcenter()
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
}

publishing.publications.getByName<MavenPublication>("kotlinMultiplatform") {
    logger.lifecycle("Updating kotlinMultiplatform publication from $groupId:$artifactId to net.devrieze:serialutil")
    groupId = "net.devrieze"
    artifactId = "serialutil"
}

publishing.publications.getByName<MavenPublication>("metadata") {
    logger.lifecycle("Updating $name publication from $groupId:$artifactId to net.devrieze:serialutil-common")
    artifactId = "serialutil-common"
}

extensions.configure<BintrayExtension>("bintray") {
    if (rootProject.hasProperty("bintrayUser")) {
        user = rootProject.property("bintrayUser") as String?
        key = rootProject.property("bintrayApiKey") as String?
    }

    val pubs = publishing.publications
//        .filter { it.name != "metadata" }
        .map { it.name }
        .apply { forEach { logger.lifecycle("Registering publication \"$it\" to Bintray") } }
        .toTypedArray()


    setPublications(*pubs)

    pkg(closureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = "net.devrieze:serialutil"
        userOrg = "pdvrieze"
        setLicenses("Apache-2.0")
        vcsUrl = "https://github.com/pdvrieze/xmlutil.git"

        version.apply {
            name = xmlutil_version
            desc = xmlutil_versiondesc
            released = Date().toString()
            vcsTag = "v$xmlutil_version"
        }
    })

}

fixBintrayModuleUpload()

idea {
    module {
        name = "xmlutil-serialutil"
    }
}
