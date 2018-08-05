import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import net.devrieze.gradle.ext.doPublish
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


val sourcesJar = task<Jar>("mySourcesJar") {
    from(java.sourceSets["main"].allSource)
}

doPublish(sourcesJar)

idea {
    module {
        name = "${parent?.name}-${project.name}"
    }
}
