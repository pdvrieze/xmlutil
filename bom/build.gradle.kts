/*
 * Copyright (c) 2025-2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import net.devrieze.gradle.ext.doPublish

plugins {
    id("projectPlugin")
    `java-platform`
    `maven-publish`
    signing
}

private val coordinatesDir = isolated.rootProject.projectDirectory.dir("build/coordinates").asFile

dependencies {
    constraints {
        if (coordinatesDir.exists()) {
            coordinatesDir.listFiles()?.forEach { file ->
                file.readLines().forEach { coordinate ->
                    if (coordinate.isNotBlank()) api(coordinate)
                }
            }
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("mavenBom") {
            from(components["javaPlatform"])

            pom {
                name = "xmlutil-Bill of Materials"
                description = "Centralised dependencies for xmlutil"
            }
        }
    }
}

doPublish(pubDescription = "Centralised dependencies for xmlutil", generateJavadoc = false)

tasks.named("generatePomFileForMavenBomPublication") {
    dependsOn(provider {


        gradle.includedBuilds
            .filter { "project-plugins" !in it.name }
            .map { it.task(":exportArtifactCoordinates")/*.toString().lines()*/ }
    })
}

tasks.withType<GenerateModuleMetadata>().configureEach {
    enabled = false
}
