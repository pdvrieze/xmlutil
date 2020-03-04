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

import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import groovy.util.Node
import groovy.xml.QName
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.artifact.FileBasedMavenArtifact
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*


inline fun XmlProvider.dependencies(config: Node.() -> Unit): Unit {
    asNode().dependencies(config)
}

inline val Node.qName: QName get() = name().let { if (it is QName) it else QName(it.toString()) }
inline fun Node.nodeChildren(): List<Node> = children() as List<Node>
fun Node.child(name:String) = nodeChildren().firstOrNull { it.qName.localPart == name }
fun Node.child(name:QName) = nodeChildren().firstOrNull { it.qName.matches(name) }

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
fun KotlinBuildScript.doPublish(sourceJar: Jar, bintrayId: String? = null) {
    val xmlutil_version: String by project
    val xmlutil_versiondesc: String by project


    if (version == "unspecified") version = xmlutil_version

    val artId = when (project.parent?.name) {
        "serialization" -> "xmlutil-serialization-${project.name}"
        else            -> "xmlutil-${project.name}"
    }

    sourceJar.classifier = "sources"

    configure<PublishingExtension> {
        (publications) {
            register<MavenPublication>("MyPublication") {
                from(components["java"])

                groupId = "net.devrieze"
                artifactId = artId
                artifact(sourceJar).apply {
                    classifier = "sources"
                }

                pom {
                    withXml {
                        dependencies {
                            nodeChildren()
                                    .filter { it.child(GROUPID)?.text()?.startsWith("xmlutil")!=false }
                                    .forEach{remove(it)}
                        }
                    }
                }
            }
        }
    }

    extensions.configure<BintrayExtension>("bintray") {

        if (rootProject.hasProperty("bintrayUser")) {
            user = rootProject.property("bintrayUser") as String?
            key = rootProject.property("bintrayApiKey") as String?
        }

        setPublications("MyPublication")

        pkg(closureOf<BintrayExtension.PackageConfig> {
            repo = "maven"
            name = bintrayId ?: artId
            userOrg = "pdvrieze"
            setLicenses("Apache-2.0")
            vcsUrl = "https://github.com/pdvrieze/xmlutil.git"

            version.apply {
                name = xmlutil_version
                desc = xmlutil_versiondesc
                released = java.util.Date().toString()
                vcsTag = "v$version"
            }
        })
    }

    tasks.withType<BintrayUploadTask> {
        dependsOn(sourceJar)
    }

}

fun Project.fixBintrayModuleUpload() {
    tasks.withType<BintrayUploadTask> {
        doFirst {
            (project.extensions.findByType<PublishingExtension>())//("publishing") as PublishingExtension?)
                ?.run {
                    this.publications
                    .filterIsInstance<MavenPublication>()
                    .forEach { publication ->
                        val moduleFile = buildDir.resolve("publications/${publication.name}/module.json")
                        if (moduleFile.exists()) {
                            publication.artifact(object : FileBasedMavenArtifact(moduleFile) {
                                override fun getDefaultExtension() = "module"
                            })
                        }
                    }
                }
        }
    }
}