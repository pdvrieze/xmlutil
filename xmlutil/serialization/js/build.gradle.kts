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

import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
//    `java-library`
    id("kotlin-platform-js")
    id("kotlinx-serialization")
}

base {
    archivesBaseName="xmlutil-serialization-js"
}


val serializationVersion:String by project
val spekVersion:String by project
val jupiterVersion:String by project

dependencies {
    implementation(project(":xmlutil:core:js"))
    implementation(project(":multiplatform:js"))
    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serializationVersion")

    expectedBy(project(":xmlutil:serialization:common"))
}


repositories {
    jcenter()
    maven(url = "https://kotlin.bintray.com/kotlinx")
}
