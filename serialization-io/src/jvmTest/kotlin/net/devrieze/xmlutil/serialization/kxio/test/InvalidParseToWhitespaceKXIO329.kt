/*
 * Copyright (c) 2026.
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

package net.devrieze.xmlutil.serialization.kxio.test

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.impl.multiplatform.InputStream
import nl.adaptivity.xmlutil.newReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlChildrenName
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy
import nl.adaptivity.xmlutil.xmlStreaming
import java.net.URI
import kotlin.test.Test

/** Test for #329 where a Maven POM file has content invalidly parsed as whitespace */
class InvalidParseToWhitespaceKXIO329 {

    private fun defaultFormat() = XML.recommended_1_0 {
        policy {
            autoPolymorphic = false
            ignoreUnknownChildren()
            encodeDefault = XmlSerializationPolicy.XmlEncodeDefault.NEVER
        }
        xmlDeclMode = XmlDeclMode.None
        defaultToGenericParser = true
    }

    private fun getInputStream(online: Boolean = true): InputStream = when (online) {
        true -> URI.create("https://repo.maven.apache.org/maven2/org/htmlunit/htmlunit/4.21.0/htmlunit-4.21.0.pom").toURL().openStream()
        else -> javaClass.getResourceAsStream("/htmlunit-4.21.0.pom")!!
    }

    @Test
    fun decodingHtmlUnitShouldWorkOnline() {
        getInputStream(true).use { ins ->
            xmlStreaming.newReader(ins).use { reader ->
                val _ = defaultFormat().decodeFromReader<MavenInfo>(reader)
            }
        }
    }

    @Test
    fun decodingHtmlUnitShouldWorkOffline() {
        getInputStream(false).use { ins ->
            xmlStreaming.newReader(ins).use { reader ->
                val _ = defaultFormat().decodeFromReader<MavenInfo>(reader)
            }
        }
    }



    @Serializable
    @SerialName("project")
    internal data class MavenInfo(
        @XmlElement(true) val name: String? = null,
        @XmlElement(true) val url: String? = null,
        @XmlChildrenName("dependency") val dependencies: List<Dependency> = emptyList(),
        val scm: SCMInfos? = null,
    )

    @Serializable
    @SerialName("dependency")
    internal data class Dependency(
        @XmlElement(true) val groupId: String,
        @XmlElement(true) val artifactId: String,
        @XmlElement(true) val version: String? = null,
    )

    @Serializable
    @SerialName("scm")
    internal data class SCMInfos(
        @XmlElement(true) val url: String? = null,
    )

}
