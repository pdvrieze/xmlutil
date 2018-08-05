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

import net.devrieze.gradle.ext.doPublish
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    id("kotlin-platform-common")
    id("kotlinx-serialization")
    id("maven-publish")
    id("com.jfrog.bintray")

}

base {
    archivesBaseName="xmlutil-core-common-nonshared"
}

val serializationVersion:String by project

dependencies {
    implementation(kotlin("stdlib-common"))
    implementation(project(":core:common"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationVersion")
}

val sourcesJar = task<Jar>("mySourcesJar") {
    from(java.sourceSets["main"].allSource)
}


doPublish(sourcesJar, "xmlutil-common")
