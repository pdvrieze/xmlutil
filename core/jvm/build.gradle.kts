import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import java.util.Date

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

plugins {
    id("java-library")
    id("kotlin-platform-jvm")
    id("idea")
    id("kotlinx-serialization")
    id("maven-publish")
    id("com.jfrog.bintray")
}

val kotlin_version:String by project
val serializationVersion:String by project
val myJavaVersion:JavaVersion by project
val xmlutil_version:String by project
val xmlutil_versiondesc:String by project

version = xmlutil_version
group = "net.devrieze"
description = "Utility classes for xml handling that works across platforms (jvm/js/android), and more powerful than jaxb"

base {
    archivesBaseName="xmlutil-core-jvm"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    expectedBy(project(":core:common-nonshared"))
    api(project(":core:java"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")

}

tasks.getByName<Jar>("jar") {
    baseName = "${project.parent?.name}-${project.name}"
}

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}


val sourcesJar = task<Jar>("androidSourcesJar") {
    classifier = "sources"
    from(java.sourceSets["main"].allSource)
}


publishing {
    (publications) {
        "MyPublication"(MavenPublication::class) {
            artifact(tasks.getByName("jar"))

            groupId = project.group as String
            artifactId = "xmlutil-jvm"
            artifact(sourcesJar).apply {
                classifier = "sources"
            }
        }
    }
}

bintray {
    if (rootProject.hasProperty("bintrayUser")) {
        user = rootProject.property("bintrayUser") as String?
        key = rootProject.property("bintrayApiKey") as String?
    }

    setPublications("MyPublication")

    pkg(closureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = "android-coroutines"
        userOrg = "pdvrieze"
        setLicenses("LGPL-3.0")
        vcsUrl = "https://github.com/pdvrieze/android-coroutines.git"

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

idea {
    module {
        name = "${parent?.name}-${project.name}"
    }
}
