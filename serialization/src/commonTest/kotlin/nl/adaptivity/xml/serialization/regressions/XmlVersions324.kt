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

package nl.adaptivity.xml.serialization.regressions

import io.github.pdvrieze.xmlutil.testutil.DocDeclEqualityMode
import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy
import kotlin.test.Test

/** Tests based upon feedback in #324 that xml versions are not respected properly */
class XmlVersions324 {

    @Test
    fun testSerializeXml10_minimal() {
        testImpl("1.0", XmlVersion.XML10, XmlDeclMode.Minimal)
    }

    @Test
    fun testSerializeXml10_none() {
        testImpl(null, XmlVersion.XML10, XmlDeclMode.None)
    }

    @Test
    fun testSerializeXml10_ifRequired() {
        testImpl(null, XmlVersion.XML10, XmlDeclMode.IfRequired)
    }

    @Test
    fun testSerializeXml11_minimal() {
        testImpl("1.1", XmlVersion.XML11, XmlDeclMode.Minimal)
    }

    @Test
    fun testSerializeXml11_none() {
        testImpl(null, XmlVersion.XML11, XmlDeclMode.None)
    }

    @Test
    fun testSerializeXml11_ifRequired() {
        testImpl("1.1", XmlVersion.XML11, XmlDeclMode.IfRequired)
    }

    private fun testImpl(versionStr: String?, versionConfig: XmlVersion, mode: XmlDeclMode = XmlDeclMode.Minimal) {
        val expected = """${versionStr?.let { "|<?xml version='$it' ?>" } ?: ""}
                |<example>
                |    <inner />
                |</example>
            """.trimMargin()

        val xml = XML.v1 {
            xmlVersion = versionConfig
            setIndent(4)
            xmlDeclMode = mode
            defaultToGenericParser = true
            policy { encodeDefault = XmlSerializationPolicy.XmlEncodeDefault.NEVER }
        }

        val encoded = xml.encodeToString(Example(""))
        assertXmlEquals(expected, encoded, ignoreDocDecl = DocDeclEqualityMode.CHECK)
    }

    @Serializable
    @XmlSerialName("example")
    class Example(@XmlElement(true) val inner: String)

}
