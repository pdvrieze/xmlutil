/*
 * Copyright (c) 2024-2025.
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

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.internal.extensions.core.extra
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension

@Suppress("LocalVariableName")
fun Project.doPublish(
    pubName: String = project.name,
    pubDescription: String = "Component of the XMLUtil library"
) {

    val isReleaseVersion = ("SNAPSHOT" !in version.toString())
    extra["isReleaseVersion"] = isReleaseVersion


    val javadocJarTask = tasks.register<Jar>("javadocJar") {
        archiveClassifier.set("javadoc")
        from(rootProject.file("README.md"))
//        from(tasks.named("dokkaGeneratePublicationHtml"))
    }

    configure<PublishingExtension> {
        this.repositories {
            maven {
                name = "projectLocal"

                setUrl(project.layout.buildDirectory.dir("project-local-repository").map { it.asFile.toURI() })
                /*
                    url = when {
                        "SNAPSHOT" in version.toString().uppercase(Locale.getDefault()) ->
                            uri("https://central.sonatype.com/repository/maven-snapshots/")
                        repositoryId != null ->
                            uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deployByRepositoryId/$repositoryId/")
                        else ->
                            uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
                    }
    */
                /*
                    credentials {
                        username = project.findProperty("ossrh.username") as String?
                        password = project.findProperty("ossrh.password") as String?
                    }
    */

            }
        }


        configure<SigningExtension> {
            val priv_key:String? = System.getenv("GPG_PRIV_KEY")
            val passphrase:String? = System.getenv("GPG_PASSPHRASE")
            when {
                priv_key != null && passphrase != null -> useInMemoryPgpKeys(priv_key, passphrase)

                System.getenv("JITPACK").equals("true", true) -> {
                    if (!rootProject.extra.has("NO_SIGNING")) {
                        logger.warn("No private key information found in environment. Running on Jitpack, skipping signing")

                        setRequired(false)
                        rootProject.extra.set("NO_SIGNING", true)
                    }
                }

                else -> {
                    logger.warn("No private key information found in environment. Falling back to gnupg.")
                    useGpgCmd()
                }
            }
        }

        publications.withType<MavenPublication> {

//            artifactId = project.name

            // the attributes aren't needed for pom (it selects the right module)
//            suppressPomMetadataWarningsFor("jvmApiElements-published")

            artifact(javadocJarTask)



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


    configure<SigningExtension> {
        when {
            rootProject.extra["NO_SIGNING"] == true ->
                setRequired(false)

            else ->
                setRequired { gradle.taskGraph.run { hasTask("publish") || hasTask("publishNative") } }

        }

        val publishing = extensions.findByType<PublishingExtension>()
        val signTasks = sign(publishing!!.publications)

        tasks.withType<AbstractPublishToMaven> {
                    val specificSignTaskName = "sign${name.substringBefore("Publication").substringAfter("publish")}Publication"
                    tasks.findByName(specificSignTaskName)?.let {
                        logger.debug("Add dependency for ${name} on ${specificSignTaskName}")
                        dependsOn(it)
                    }
            dependsOn(signTasks)
        }

    }

    val publishNativeTask = tasks.create<Task>("publishNative") {
        group = "Publishing"
        description = "Task to publish all native artefacts only"
    }



    tasks.withType<PublishToMavenRepository> {
        if (isEnabled) {

            if (repository?.name == "projectLocal") {
                val repositoryDir = project.layout.buildDirectory.dir("project-local-repository")
                if (repositoryDir.isPresent) {
                    repositoryDir.get().asFile.deleteRecursively()
                }

                val publishTask = this

                rootProject.tasks.named<Zip>("collateModuleRepositories") {
                    dependsOn(publishTask)
                    from(repositoryDir)
                }
            }

            val doPublish = arrayOf("publishKotlinMultiplatform", "publishJs", "publishJvm", "publishAndroid").none { "${it}Publication" in name }
            if (doPublish) {
                publishNativeTask.dependsOn(this)
            }
        }
    }

/*
    tasks.withType<Sign>().configureEach {
        onlyIf { project.hasProperty("isReleaseVersion") && (project.extra["isReleaseVersion"] as Boolean) }
    }
*/

}
