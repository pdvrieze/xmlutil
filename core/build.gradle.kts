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
    archivesBaseName = "xmlutil"
    version = xmlutil_version
}

val serializationVersion: String by project

val kotlin_version: String by project

val jupiterVersion: String by project

val androidAttribute = Attribute.of("net.devrieze.android", Boolean::class.javaObjectType)

val moduleName = "net.devrieze.xmlutil.core"

kotlin {
    targets {
        val testTask = tasks.create("test") {
            group = "verification"
        }
        val cleanTestTask = tasks.create("cleanTest") {
            group = "verification"
        }

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
                tasks.named<Test>("${target.name}Test") {
                    useJUnitPlatform()
                    testTask.dependsOn(this)
                }
                cleanTestTask.dependsOn(tasks.getByName("clean${target.name[0].toUpperCase()}${target.name.substring(1)}Test"))
                tasks.named<Jar>("jvmJar") {
                    manifest {
                        attributes("Automatic-Module-Name" to moduleName)
                    }
                }
//                tasks.named<Jar>()
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
                }
                tasks.getByName<Test>("${target.name}Test") {
                    useJUnitPlatform ()
                    testTask.dependsOn(this)
                }
                cleanTestTask.dependsOn(tasks.getByName("clean${target.name[0].toUpperCase()}${target.name.substring(1)}Test"))
            }
        }
        js {
            browser()
            compilations.all {
                tasks.getByName<KotlinJsCompile>(compileKotlinTaskName).kotlinOptions {
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

        target.mavenPublication {
            groupId = "net.devrieze"
            artifactId = "xmlutil-${target.targetName}"
            version = xmlutil_version
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val javaShared by creating {
            dependsOn(commonMain)
        }

        val jvmMain by getting {
            dependsOn(javaShared)
        }

        val jvmTest by getting {
            dependencies {
                dependsOn(commonTest)
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")

                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
                runtimeOnly("com.fasterxml.woodstox:woodstox-core:5.0.3")
            }
        }

        val androidMain by getting {
            dependsOn(javaShared)

            dependencies {
                compileOnly("net.sf.kxml:kxml2:2.3.0")
            }
        }

        val androidTest by getting {
            dependencies {
                dependsOn(commonTest)

                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")

                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
                runtimeOnly("net.sf.kxml:kxml2:2.3.0")
            }
        }

        val jsMain by getting {
            dependsOn(commonMain)
        }

        val jsTest by getting {
            dependsOn(commonTest)
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

    }

}


repositories {
    jcenter()
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
}

publishing.publications.getByName<MavenPublication>("kotlinMultiplatform") {
    logger.lifecycle("Updating kotlinMultiplatform publication from $groupId:$artifactId to net.devrieze:xmlutil")
    groupId = "net.devrieze"
    artifactId = "xmlutil"
}

publishing.publications.getByName<MavenPublication>("metadata") {
    logger.lifecycle("Updating $name publication from $groupId:$artifactId to net.devrieze:xmlutil-common")
    artifactId = "xmlutil-common"
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
        name = "net.devrieze:xmlutil"
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
        name = "xmlutil-core"
    }
}
