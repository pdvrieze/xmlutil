import net.devrieze.gradle.ext.doPublish

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
    base
    `java-library`
    id("kotlin-platform-jvm")
    id("kotlin-kapt")
    idea
    id("kotlinx-serialization")
    id("maven-publish")
    id("com.jfrog.bintray")
}

@Suppress("PropertyName")
val kotlin_version: String by rootProject
val kotlinVersion get() = kotlin_version as String?
val serializationVersion:String by project
val xmlutil_version:String by project
val x: Build_gradle = this
base {
    archivesBaseName="xmlutil-core-android"
}


version = xmlutil_version
description = "Utility classes for xml handling that works across platforms (jvm/js/android), and more powerful than jaxb"


dependencies {
    compileOnly("net.sf.kxml:kxml2:2.3.0")
    testRuntime("net.sf.kxml:kxml2:2.3.0")

    expectedBy(project(":core:common-nonshared"))
    api(project(":core:java"))
    implementation(kotlin("stdlib", kotlinVersion))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
}


val sourcesJar = task<Jar>("androidSourcesJar") {
    from(java.sourceSets["main"].allSource)
}

doPublish(sourcesJar)

idea {
    module {
        name = "${parent?.name}-${project.name}"
    }
}
