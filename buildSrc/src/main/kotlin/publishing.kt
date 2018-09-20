/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package net.devrieze.gradle.ext

import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import groovy.lang.Closure
import groovy.util.Node
import groovy.xml.QName
import org.gradle.api.Buildable
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.component.Component
import org.gradle.api.file.FileTree
import org.gradle.api.internal.file.CompositeFileTree
import org.gradle.api.logging.Logging
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskDependency
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.DependentSourceSet



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

fun KotlinBuildScript.doPublish(sourceJar: Jar, bintrayId: String? = null) {
    val xmlutil_version: String by project
    val xmlutil_versiondesc: String by project


    if (version == "unspecified") version = xmlutil_version

    val artId = when (project.parent?.name) {
        "serialization" -> "xmlutil-serialization-${project.name}"
        else            -> "xmlutil-${project.name}"
    }

    sourceJar.classifier = "sources"

    extensions.configure<PublishingExtension>("publishing") {
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
            setLicenses("LGPL-3.0")
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
