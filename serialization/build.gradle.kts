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
import org.jetbrains.kotlin.psi.psiUtil.checkReservedPrefixWord
import java.util.*

plugins {
    kotlin("multiplatform")
    id("kotlinx-serialization")
    id("maven-publish")
    id("com.jfrog.bintray")
//    id("com.moowork.node") version "1.3.1"
    idea
}

val xmlutil_version: String by project
val xmlutil_versiondesc: String by project

base {
    archivesBaseName = "xmlutil-serialization"
    version = xmlutil_version
}

val serializationVersion: String by project
val spek2Version: String by project
val jupiterVersion: String by project

val kotlin_version: String by project
val androidAttribute = Attribute.of("net.devrieze.android", Boolean::class.javaObjectType)

val moduleName = "net.devrieze.xmlutil.serialization"


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
                attribute(androidAttribute, false)
                attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
            }
            compilations.all {
                tasks.named<KotlinCompile>(compileKotlinTaskName) {
                    kotlinOptions {
                        jvmTarget = "1.8"
                        freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
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
            }
        }
        jvm("android") {
            attributes {
                attribute(androidAttribute, true)
                attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
                attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 6)
            }
            compilations.all {
                tasks.getByName<KotlinCompile>(compileKotlinTaskName).kotlinOptions {
                    jvmTarget = "1.6"
                    freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
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
                val compileTask = tasks.getByName<KotlinJsCompile>(compileKotlinTaskName).apply {
                    kotlinOptions {
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

        forEach { target ->
            target.mavenPublication {
                groupId = "net.devrieze"
                artifactId = "xmlutil-serialization-${target.targetName}"
                version = xmlutil_version
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core"))
                implementation(project(":serialutil"))
                project.dependencies.add(apiConfigurationName,
                "org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationVersion") {
                    exclude(group = "org.jetbrains.kotlin")
                }
                implementation(kotlin("stdlib"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
//                implementation("org.spekframework.spek2:spek-dsl-common:$spek2Version")
            }
        }
        val javaShared by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(kotlin("stdlib-jdk7"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
            }
        }
        val javaSharedTest by creating {
            dependsOn(javaShared)
            dependsOn(commonTest)
        }
        val jvmMain by getting {
            dependsOn(javaShared)
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        val jvmTest by getting {
            dependencies {
                dependsOn(javaSharedTest)
                implementation(project(":core"))
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")

                implementation("org.spekframework.spek2:spek-dsl-jvm:$spek2Version")
                runtimeOnly("org.spekframework.spek2:spek-runtime-jvm:$spek2Version")
                runtimeOnly("org.spekframework.spek2:spek-runner-junit5:$spek2Version")


                implementation("org.xmlunit:xmlunit-core:2.6.0")

                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
                implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
                runtimeOnly("com.fasterxml.woodstox:woodstox-core:5.0.3")

            }
        }
        val androidMain by getting {
            dependsOn(javaShared)
            dependencies {
                implementation(project(":core"))
                compileOnly("net.sf.kxml:kxml2:2.3.0")
            }
        }
        val androidTest by getting {
            dependencies {
                dependsOn(javaSharedTest)
                implementation(project(":core"))
                implementation(kotlin("test-junit5"))
                runtimeOnly("net.sf.kxml:kxml2:2.3.0")

                implementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")

                implementation(kotlin("stdlib-jdk8"))

//                implementation("org.spekframework.spek2:spek-dsl-jvm:$spek2Version")
//                runtimeOnly("org.spekframework.spek2:spek-runner-junit5:$spek2Version")

                implementation("org.xmlunit:xmlunit-core:2.6.0")

                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")

                implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")

            }
        }
        val jsMain by getting {
            //            dependsOn(commonMain)
            dependencies {
                api(project(":core"))
                implementation(project(":serialutil"))
                implementation("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlin_version")
                project.dependencies.add(
                    apiConfigurationName,
                    "org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serializationVersion") {
                    exclude(group = "org.jetbrains.kotlin")
                }
            }
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

publishing.publications.named<MavenPublication>("kotlinMultiplatform") {
    logger.lifecycle("Updating kotlinMultiplatform publication from $groupId:$artifactId to net.devrieze:xmlutil-serialization")
    groupId = "net.devrieze"
    artifactId = "xmlutil-serialization"
}

publishing.publications.getByName<MavenPublication>("metadata") {
    logger.lifecycle("Updating $name publication from $groupId:$artifactId to net.devrieze:xmlutil-serialization-common")
    artifactId = "xmlutil-serialization-common"
}

tasks.named("check") {
    dependsOn(tasks.named("test"))
}

tasks.withType<Test>() {
    logger.lifecycle("Enabling xml reports on task ${project.name}:${name}")
    reports {
        junitXml.isEnabled = true
    }
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
        name = "net.devrieze:xmlutil-serialization"
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
    this.module.name = "xmlutil-serialization"
}
