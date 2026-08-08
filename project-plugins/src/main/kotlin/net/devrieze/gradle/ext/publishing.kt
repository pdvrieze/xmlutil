/*
 * Copyright (c) 2024-2026.
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

package net.devrieze.gradle.ext

import io.github.xmlutil.plugin.isSnapshot
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension

fun Project.doPublish(
    pubName: String = project.name,
    pubDescription: String = "Component of the XMLUtil library",
    generateJavadoc: Boolean = true,
) {
    configureSigningOfPublications()

    configure<PublishingExtension> {
        repositories {
            if (isSnapshot) {
                maven {
                    name = "mavenSnapshot"
                    url = uri("https://central.sonatype.com/repository/maven-snapshots/")
                    credentials {
                        username = project.findProperty("ossrh.username") as String?
                        password = project.findProperty("ossrh.password") as String?
                    }
                }
                maven {
                    name = "testMavenSnapshot"

                    @Suppress("UnstableApiUsage")
                    url = isolated.rootProject.projectDirectory.dir("build/testMavenSnapshot").asFile.toURI()
                }
            }
            maven {
                name = "projectLocal"

                @Suppress("UnstableApiUsage")
                url = isolated.rootProject.projectDirectory.dir("build/project-local-repository").asFile.toURI()
            }

        }

        publications.withType<MavenPublication>().configureEach {

            val publication = this

            if (generateJavadoc && name != "kotlinMultiplatform" && !isSnapshot) {

                val javadocJarTaskName = "${name}JavadocJar"
                val javadocJarTask = project.tasks.register<Jar>(javadocJarTaskName) {
                    archiveBaseName = publication.name
                    archiveClassifier = "javadoc"
                    from(project.rootProject.file("README.md"))
                }

                artifact(javadocJarTask)
            }

            pom {
                name = pubName
                description.set(pubDescription)
                url = "https://github.com/pdvrieze/xmlutil"

                licenses {
                    license {
                        name = "Apache-2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id.set("pdvrieze")
                        name.set("Paul de Vrieze")
                        email.set("paul.devrieze@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/pdvrieze/xmlutil.git")
                    developerConnection.set("scm:git:https://github.com/pdvrieze/xmlutil.git")
                    url.set("https://github.com/pdvrieze/xmlutil")
                }
            }
        }

    }

    recordPublicationCoordinates()

    if (isSnapshot) {
        tasks.withType<PublishToMavenRepository>().configureEach {
            doFirst {
                val pubArtifacts = publication.artifacts

                pubArtifacts.removeIf { artifact ->
                    artifact.classifier == "sources"
                }
            }
        }
    }

    val publishNativeTask = tasks.register<Task>("publishNative") {
        group = "Publishing"
        description = "Task to publish all native artefacts only"

        val dependencies = tasks.matching {
            it is PublishToMavenRepository && arrayOf(
                "publishKotlinMultiplatform",
                "publishJs",
                "publishJvm",
                "publishAndroid"
            ).none { "${it}Publication" in name }
        }
        dependsOn(dependencies)
    }

    val cleanLocalRepoTask = ":cleanLocalRepo"

    tasks.withType<PublishToMavenRepository>().matching { it.repository?.name == "projectLocal" }.configureEach {
        if (isEnabled) dependsOn(cleanLocalRepoTask)
    }


}

private fun Project.recordPublicationCoordinates() {
    if (name == "xmlutil-bom") return

    val projectName = name

    configure<PublishingExtension> {
        @Suppress("UnstableApiUsage")
        val coordinateFolder = isolated.rootProject.projectDirectory.dir("build/coordinates")

        val exportCoordinatesTask = tasks.register("exportArtifactCoordinates") {
            val publications = publications
            val outputFile = coordinateFolder.file("${projectName}.txt").asFile
            outputs.file(outputFile)

            doLast {
                coordinateFolder.asFile.mkdirs()
                outputFile.bufferedWriter().use { writer ->
                    for(p in publications) {
                        if (p is MavenPublication && !(p.artifactId.endsWith("-metadata") ||
                                    p.artifactId.endsWith("-kotlinMultiplatform") ||
                                    p.artifacts.any { it.extension == "klib" }
                                    )) {

                            writer.write("${p.groupId}:${p.artifactId}:${p.version}\n")
                        }
                    }
                }
            }
        }

        tasks.matching { name.startsWith("generatePomFileFor") && name.endsWith("Publication") }.configureEach() {
            dependsOn(exportCoordinatesTask)
        }
    }
}

fun Project.configureSigningOfPublications() {
    configure<SigningExtension> {
        val priv_key: String? = System.getenv("GPG_PRIV_KEY")
        val passphrase: String? = System.getenv("GPG_PASSPHRASE")
        var noSigning = false
        when {
            priv_key != null && passphrase != null -> useInMemoryPgpKeys(priv_key, passphrase)

            System.getenv("JITPACK").equals("true", true) -> {
                logger.info("No private key information found in environment. Running on Jitpack, skipping signing")

                setRequired(false)
                noSigning = true
            }

            else -> {
                logger.warn("No private key information found in environment. Falling back to gnupg.")
                useGpgCmd()
            }
        }

        extensions.findByType<PublishingExtension>()?.run {
            sign(publications)
        }

        when {
            noSigning -> setRequired(false)

            else ->
                setRequired { gradle.taskGraph.run { hasTask("publish") || hasTask("publishNative") } }

        }
    }
}
