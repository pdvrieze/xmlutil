/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package net.devrieze.gradle.ext

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

@Suppress("LocalVariableName")
fun Project.doPublish(
    pubName: String = project.displayName,
    pubDescription: String = "Component of the XMLUtil library"
) {
    val xmlutil_version: String by project

    if (version == "unspecified") version = xmlutil_version

    val isReleaseVersion = ("SNAPSHOT" !in xmlutil_version)
    extra["isReleaseVersion"] = isReleaseVersion

    configure<PublishingExtension> {
        repositories {
            maven {
                name = "OSS_registry"
                val repositoryId = project.properties["xmlutil.repositoryId"] as String?
                url = when {
                    "SNAPSHOT" in version.toString().toUpperCase() ->
                        uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                    repositoryId != null ->
                        uri("https://s01.oss.sonatype.org/service/local/staging/deployByRepositoryId/$repositoryId/")
                    else ->
                        uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                }
                credentials {
                    username = project.findProperty("ossrh.username") as String?
                    password = project.findProperty("ossrh.password") as String?
                }

            }
        }

        configure<SigningExtension> {
            val priv_key:String? = System.getenv("GPG_PRIV_KEY")
            val passphrase:String? = System.getenv("GPG_PASSPHRASE")
            if (priv_key==null ||passphrase==null) {
                logger.warn("No private key information found in environment. Falling back to gnupg.")
                useGpgCmd()
            } else {
                useInMemoryPgpKeys(priv_key, passphrase)
            }
        }

        val javadocJarTask = tasks.create<Jar>("javadocJar") {
            archiveClassifier.set("javadoc")
            from(tasks.named("dokkaHtml"))
        }

        publications.withType<MavenPublication> {
            artifact(javadocJarTask)

            val pub = this
            configure<SigningExtension> {
                setRequired { gradle.taskGraph.run { hasTask("publish") || hasTask("publishNative") } }

                sign(pub)
            }
            pom {
                name.set(pubName)
                description.set(pubDescription)
                url.set("https://github.com/pdvrieze/xmlutil")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
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

    val publishNativeTask = tasks.create<Task>("publishNative") {
        group = "Publishing"
        description = "Task to publish all native artefacts only"
    }

    tasks.withType<PublishToMavenRepository> {
        if (isEnabled) {
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
