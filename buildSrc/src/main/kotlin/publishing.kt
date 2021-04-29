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

import groovy.util.Node
import groovy.xml.QName
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.Sign
import org.gradle.kotlin.dsl.extra


inline fun XmlProvider.dependencies(config: Node.() -> Unit): Unit {
    asNode().dependencies(config)
}

inline val Node.qName: QName
    get() = name().let {
        if (it is QName) it else QName(
            it.toString()
                                      )
    }

inline fun Node.nodeChildren(): List<Node> = children() as List<Node>
fun Node.child(name: String) =
    nodeChildren().firstOrNull { it.qName.localPart == name }

fun Node.child(name: QName) =
    nodeChildren().firstOrNull { it.qName.matches(name) }

private const val POM_NS = "http://maven.apache.org/POM/4.0.0"
val DEPENDENCIES = QName(POM_NS, "dependencies")
val GROUPID = QName(POM_NS, "groupId")

inline fun Node.dependencies(config: Node.() -> Unit): Node {
    val node: Node = child(DEPENDENCIES) ?: appendNode(DEPENDENCIES)
    return node.apply(config)
}
/*
fun Node.dependency(spec: String, type: String = "jar", scope: String = "compile", optional: Boolean = false): Node {
    return spec.split(':', limit = 3).run {
        val groupId = get(0)
        val artifactId = get(1)
        val version = get(2)
        dependency(groupId, artifactId, version, type, scope, optional)
    }
}

fun Node.dependency(groupId: String,
                    artifactId: String,
                    version: String,
                    type: String = "jar",
                    scope: String = "compile",
                    optional: Boolean = false): Node {
    return appendNode("dependency").apply {
        appendNode("groupId", groupId)
        appendNode("artifactId", artifactId)
        appendNode("version", version)
        appendNode("type", type)
        if (scope != "compile") appendNode("scope", scope)
        if (optional) appendNode("optional", "true")
    }
}
 */

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
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/pdvrieze/xmlutil")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                        ?: project.findProperty("gpr.user") as String?
                                ?: System.getenv("USERNAME")
                    password = System.getenv("GITHUB_TOKEN")
                        ?: project.findProperty("gpr.key") as String?
                                ?: System.getenv("TOKEN")
                }
            }
            maven {
                if ("SNAPSHOT" in version.toString().toUpperCase()) {
                    name = "OSS_Snapshot_registry"
                    url =
                        uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                } else {
                    name = "OSS_Release_Staging_registry"
                    url =
                        uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                }
                credentials {
                    username = project.findProperty("ossrhUsername") as String?
                    password = project.findProperty("ossrhPassword") as String?
                }

            }
        }
        configure<SigningExtension> {
            useGpgCmd()
        }
        val javadocJarTask = tasks.create<Jar>("javadocJar") {
            archiveClassifier.set("javadoc")
            from(tasks.named("dokkaHtml"))
        }
        publications.withType<MavenPublication> {
            artifact(javadocJarTask)

            val pub = this
            configure<SigningExtension> {
                setRequired({(project.extra["isReleaseVersion"] as Boolean) &&
                        gradle.taskGraph.hasTask("publishAllPublicationsToOSS_Release_Staging_registryRepository") &&
                        System.getenv("CI")!="true" }
                           )
                if (System.getenv("GITHUB_JOB").isNullOrEmpty()) { sign(pub) }
            }
            pom {
                name.set(pubName)
                description.set(pubDescription)
                url.set("https://github.com/pdvrieze/xmlutil")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
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

    tasks.withType<Sign>().configureEach {
        onlyIf { project.hasProperty("isReleaseVersion") && (project.extra["isReleaseVersion"] as Boolean) }
    }

}
