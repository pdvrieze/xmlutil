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
    `java-library`
    id("kotlin")
//    id("kotlin-platform-jvm")
    id("kotlinx-serialization")
}

base {
    archivesBaseName="xmlutil-serialization-android-test"
}


val serializationVersion:String by project
val spekVersion:String by project
val jupiterVersion:String by project

dependencies {
    implementation(kotlin("stdlib"))
    implementation("net.sf.kxml:kxml2:2.3.0")
    implementation(project(":core:android"))
    implementation(project(":serialization:java"))

    testImplementation("org.jetbrains.spek:spek-subject-extension:${spekVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")

    testImplementation("org.xmlunit:xmlunit-core:2.6.0")


    testRuntime("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
    testImplementation(kotlin("reflect"))

    testRuntime ("org.jetbrains.spek:spek-junit-platform-engine:${spekVersion}") {
        exclude(group="org.junit.platform")
        exclude(group="org.jetbrains.kotlin")
    }

}


repositories {
    jcenter()
    maven(url = "https://kotlin.bintray.com/kotlinx")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
