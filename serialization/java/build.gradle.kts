/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import groovy.util.Node
import net.devrieze.gradle.ext.*
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Date

plugins {
    id("kotlin-platform-jvm")
    `java-library`
    id("kotlinx-serialization")
    id("maven-publish")
    id("com.jfrog.bintray")
}

base {
    archivesBaseName="xmlutil-serialization-jvm"
}


val xmlutil_version: String by project
val xmlutil_versiondesc: String by project


if (version == "unspecified") version = xmlutil_version
group = "net.devrieze.serialization"
description = "Serializer for XML based on kotlinx.serialization"

val serializationVersion:String by project
val spekVersion:String by project
val jupiterVersion:String by project

dependencies {
    implementation(project(":core:java"))
    compileOnly(project(":core:jvm"))
    api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")

    expectedBy(project(":serialization:common"))


    testImplementation("org.spekframework.spek2:spek-dsl-jvm:${spekVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")

    testImplementation("org.xmlunit:xmlunit-core:2.6.0")
    testImplementation(project(":core:jvm"))


    testRuntime("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
    testRuntime("com.fasterxml.woodstox:woodstox-core:5.0.3")
    testImplementation(kotlin("reflect"))

    testRuntime ("org.spekframework.spek2:spek-runner-junit5:${spekVersion}") {
        exclude(group="org.junit.platform")
        exclude(group="org.jetbrains.kotlin")
    }

}


repositories {
    jcenter()
    maven(url = "https://kotlin.bintray.com/kotlinx")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}



val sourcesJar = task<Jar>("mySourcesJar") {
    from(sourceSets["main"].allSource)
    classifier="sources"
}

publishing {
    (publications) {
        for (suffix in listOf("jvm", "android")) {
            val artId = "xmlutil-serialization-$suffix"
            register<MavenPublication>("${suffix}Publication") {
                from(components["java"])

                groupId = "net.devrieze"
                artifactId = artId
                artifact(sourcesJar)
                pom {
                    withXml {
                        dependencies {
                            // Drop common (nonpublished) modules dependencies
                            nodeChildren()
                                    .filter { it.child(GROUPID)?.text()?.startsWith("xmlutil") != false }
                                    .forEach { remove(it) }

                            // Replace the dependency with a platform specific one
                            nodeChildren()
                                    .asSequence()
                                    .mapNotNull { it.child("artifactId") }
                                    .filter { it.text() == "xmlutil-java" }
                                    .forEach { it: Node -> it.setValue("xmlutil-$suffix") }
                        }
                    }
                }

            }


        }
    }
}

bintray {

    if (rootProject.hasProperty("bintrayUser")) {
        user = rootProject.property("bintrayUser") as String?
        key = rootProject.property("bintrayApiKey") as String?
    }

    setPublications("jvmPublication", "androidPublication")

    pkg(closureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = "xmlutil-serialization"
        userOrg = "pdvrieze"
        setLicenses("LGPL-3.0")
        vcsUrl = "https://github.com/pdvrieze/xmlutil.git"

        version.apply {
            name = xmlutil_version
            desc = xmlutil_versiondesc
            released = Date().toString()
            vcsTag = "v$version"
        }
    })
}

tasks.withType<BintrayUploadTask> {
    dependsOn(sourcesJar)
}
