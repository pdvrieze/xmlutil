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
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTestRun
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("kotlinx-serialization")
    id("maven-publish")
    id("signing")
    idea
    id("org.jetbrains.dokka")
}

val xmlutil_serial_version: String by project
val xmlutil_core_version: String by project
val xmlutil_versiondesc: String by project

base {
    archivesBaseName = "xmlutil-serialization"
    version = xmlutil_serial_version
}

val serializationVersion: String by project
val jupiterVersion: String by project

val kotlin_version: String by project
val androidAttribute =
    Attribute.of("net.devrieze.android", Boolean::class.javaObjectType)

val moduleName = "net.devrieze.xmlutil.serialization"


kotlin {
    targets {
        val testTask = tasks.create("test") {
            group = "verification"
        }
        jvm {
            attributes {
                attribute(androidAttribute, false)
                attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
            }
            compilations.all {
                logger.lifecycle("JVM compilation: ${name}")
                tasks.named<KotlinCompile>(compileKotlinTaskName) {
                    kotlinOptions {
                        jvmTarget = "1.8"
                    }
                    if ("test" in name.toLowerCase()) {
                        logger.lifecycle("Configuring IR when testing for task ${project.name}:$name")
                        kotlinOptions.useIR = true
                    }
                }
                tasks.named<Test>("${target.name}Test") {
                    useJUnitPlatform()
                    testTask.dependsOn(this)
                }
                tasks.named<Jar>("jvmJar") {
                    manifest {
                        attributes("Automatic-Module-Name" to moduleName)
                    }
                }
            }

            val woodstoxCompilation = compilations.register("woodstoxTest")
            val woodstoxTestRun = testRuns.create("woodstoxTest") {
                setExecutionSourceFrom(
                    listOf(compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)),
                    listOf(
                        woodstoxCompilation.get()
                          )
                                      )

                executionTask.configure {
                    useJUnitPlatform()
                }
            }


        }
        jvm("android") {
            attributes {
                attribute(androidAttribute, true)
                attribute(
                    KotlinPlatformType.attribute,
                    KotlinPlatformType.androidJvm
                         )
                attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 6)
            }
            compilations.all {
                kotlinOptions.jvmTarget = "1.6"
                tasks.getByName<Test>("${target.name}Test") {
                    useJUnitPlatform()
                    testTask.dependsOn(this)
                    kotlinOptions.useIR = true
                }
            }
        }
        js(BOTH) {
            browser()
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
                languageVersion = "1.5"
                apiVersion = "1.5"
            }
        }

        target.mavenPublication {
            version = xmlutil_serial_version
        }
    }


    sourceSets {

        val commonMain by getting {
            dependencies {
                api(project(":core"))

                api("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
            }
        }

        val commonTest by getting {
            languageSettings.enableLanguageFeature("InlineClasses")

            dependencies {
                implementation(project(":serialutil"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val javaShared by creating {
            dependsOn(commonMain)
        }

        val javaSharedTest by creating {
            languageSettings.enableLanguageFeature("InlineClasses")

            dependsOn(javaShared)
            dependsOn(commonTest)
        }

        val jvmMain by getting {
            dependsOn(javaShared)
            dependencies {
                implementation(kotlin("stdlib-jdk8", kotlin_version))
            }
        }

        val jvmTestCommon by creating {
            languageSettings.enableLanguageFeature("InlineClasses")
            dependsOn(javaSharedTest)
            dependsOn(jvmMain)

            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")

                implementation("net.bytebuddy:byte-buddy:1.10.10")
                implementation("org.assertj:assertj-core:3.16.1")
                implementation("org.xmlunit:xmlunit-core:2.7.0")
                implementation("org.xmlunit:xmlunit-assertj:2.7.0")

                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
                implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
            }
        }

        val jvmTest by getting {
            languageSettings.enableLanguageFeature("InlineClasses")

            dependsOn(jvmTestCommon)
        }
        val jvmWoodstoxTest by getting {
            languageSettings.enableLanguageFeature("InlineClasses")
            dependsOn(jvmTestCommon)
            dependencies {
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
            languageSettings.enableLanguageFeature("InlineClasses")

            dependsOn(javaSharedTest)
            dependsOn(androidMain)

            dependencies {
                implementation(kotlin("test-junit5"))
                runtimeOnly("net.sf.kxml:kxml2:2.3.0")

                implementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")
                implementation("org.xmlunit:xmlunit-core:2.6.0")
                implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")

                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
            }
        }

        val jsMain by getting {
        }

        val jsTest by getting {
            languageSettings.enableLanguageFeature("InlineClasses")

            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        all {
            languageSettings.apply {
                useExperimentalAnnotation("kotlin.RequiresOptIn")
            }
        }
    }

}

doPublish()

configureDokka(myModuleVersion = xmlutil_serial_version)

tasks.register("cleanTest") {
    group = "verification"
    dependsOn(tasks.named("cleanAllTests"))
}

tasks.withType<Test> {
    logger.lifecycle("Enabling xml reports on task ${project.name}:${name}")
    reports {
        junitXml.isEnabled = true
    }
}

idea {
    this.module.name = "xmlutil-serialization"
}
