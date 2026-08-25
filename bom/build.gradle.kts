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

@file:Suppress("UnstableApiUsage")

import net.devrieze.gradle.ext.doPublish

plugins {
    id("projectPlugin")
    `java-platform`
    `maven-publish`
    signing
}

private val coordinatesDir = isolated.rootProject.projectDirectory.dir("build/coordinates").asFile

val coordinates = configurations.create("coordinates") {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, "BOM-coordinate"))
    }
}


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

    coordinates(projects.core)
    coordinates(projects.coreAndroid)
    coordinates(projects.coreJdk)
    coordinates(projects.coreIo)
    coordinates(projects.serialization)
    coordinates(projects.serializationIo)
    coordinates(projects.serialutil)
    coordinates(projects.xmlserializable)
}

publishing {
    publications {
        register<MavenPublication>("mavenBom") {
            from(components["javaPlatform"])

            pom {
                name = "xmlutil-Bill of Materials"
                description = "Centralised dependencies for xmlutil"

                withXml {
                    // Resolve the configuration safely during execution
                    val resolvedFiles = coordinates.incoming.files

                    val dependenciesNode = asNode().appendNode("dependencyManagement").appendNode("dependencies")

                    // Read every coordinate file gathered from the subprojects
                    resolvedFiles.forEach { file ->
                        file.readLines().forEach { coordinate ->
                            if (coordinate.isNotBlank()) {
                                // Inject the constraints dynamically into the generated XML file
                                val parts = coordinate.split(":")
                                val depNode = dependenciesNode.appendNode("dependency")
                                depNode.appendNode("groupId", parts[0])
                                depNode.appendNode("artifactId", parts[1])
                                depNode.appendNode("version", parts[2])
                            }
                        }
                    }
                }
            }
        }
    }
}

doPublish(pubDescription = "Centralised dependencies for xmlutil", generateJavadoc = false)

tasks.named("generatePomFileForMavenBomPublication") {
    dependsOn(coordinates)
}

tasks.withType<GenerateModuleMetadata>().configureEach {
    enabled = false
}
