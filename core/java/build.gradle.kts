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
import net.devrieze.gradle.ext.*
import java.util.*

plugins {
    id("java-library")
    id("kotlin-platform-jvm")
    id("kotlinx-serialization")
    id("maven-publish")
    id("com.jfrog.bintray")
}

base {
    archivesBaseName="xmlutil-core-java"
}

val serializationVersion:String by project

description="Shared code between the Android and Kotlin implementations of xmlutil"

dependencies {
    expectedBy(project(":core:common"))
    implementation(kotlin("stdlib-jdk7"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
}


val sourcesJar = task<Jar>("mySourcesJar") {
    from(sourceSets["main"].allSource)
}

doPublish(sourcesJar)

