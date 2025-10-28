/*
 * Copyright (c) 2025.
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

import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication

plugins {
    `java-platform`
    `maven-publish`
}

dependencies {
    constraints {
        rootProject.subprojects.asSequence()
            .filter { it.name != project.name }
            .filter { it.plugins.hasPlugin("maven-publish") }
            .forEach { subproject: Project ->
                evaluationDependsOn(subproject.path)
                for(p in subproject.publishing.publications) {
                    p as MavenPublication
                    if (!(p.artifactId.endsWith("-metadata") ||
                            p.artifactId.endsWith("-kotlinMultiplatform") ||
                            p.artifacts.any { it.extension == "klib" }
                            )) {
                        this@constraints.api(
                            mapOf(
                                "group" to p.groupId,
                                "name" to p.artifactId,
                                "version" to p.version
                            )
                        )
                    }
                }
            }
    }
}

publishing {
    publications {
        val mavenBom by creating(MavenPublication::class) {
            from(components["javaPlatform"])
        }

        for(pub in this) {
            pub as DefaultMavenPublication

            pub.unsetModuleDescriptorGenerator()

            tasks.configureEach {
                if (name == "generateMetadataFileFor${pub.name.replaceFirstChar { it.titlecase() }}") {
                    onlyIf { false }
                }
            }
        }
    }
}

fun DefaultMavenPublication.unsetModuleDescriptorGenerator() {
    @Suppress("NULL_FOR_NONNULL_TYPE")
    this.setModuleDescriptorGenerator(null)
}
