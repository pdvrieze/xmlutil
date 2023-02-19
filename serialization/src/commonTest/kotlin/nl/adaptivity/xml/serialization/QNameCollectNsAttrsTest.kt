/*
 * Copyright (c) 2021.
 *
 * This file is part of xmlutil.
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

@file:UseSerializers(QNameSerializer::class)
@file:OptIn(ExperimentalXmlUtilApi::class)

package nl.adaptivity.xml.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.*

import kotlin.test.Test
import kotlin.test.assertContains

class QNameCollectNsAttrsTest : PlatformTestBase<QNameCollectNsAttrsTest.Container>(
    Container(Child1(Child2(QName("urn:foo", "bar", "baz")))),
    Container.serializer(),
    baseXmlFormat = XML { isCollectingNSAttributes = true },
    baseJsonFormat = Json { encodeDefaults = false }
) {
    override val expectedXML: String =
        "<container xmlns=\"urn:example.org\" xmlns:prefix2=\"urn:example.org/3\" xmlns:prefix3=\"urn:example.org/4\" xmlns:baz=\"urn:foo\"><prefix2:child1><prefix3:child2><prefix3:child>baz:bar</prefix3:child></prefix3:child2></prefix2:child1></container>"
    override val expectedJson: String =
        "{\"child\":{\"child\":{\"child\":{\"namespace\":\"urn:foo\",\"localPart\":\"bar\",\"prefix\":\"baz\"}}}}"

    @Test
    override fun testGenericSerializeXml() {
        super.testGenericSerializeXml()
    }

    @Test
    fun testNamespaceDecls() {
        val xml = XML { isCollectingNSAttributes = false }
        val serialized = xml.encodeToString(serializer, value)
        assertContains(serialized, "xmlns:prefix5=\"urn:example.org/5\"")
        assertContains(serialized, "xmlns:prefix6=\"urn:example.org/6\"")
    }

    enum class AddresStatus { VALID, INVALID, TEMPORARY }

    @Serializable
    @XmlSerialName("child1", namespace = "urn:example.org/3", prefix = "prefix2")
    @XmlNamespaceDeclSpec("prefix2=urn:example.org/5")
    data class Child1(val child: Child2, val otherChild: Child1? = null)

    @Serializable
    @XmlSerialName("child2", namespace = "urn:example.org/4", prefix = "prefix3")
    data class Child2(@XmlElement(true) val child: QName, val nestedContainer: Container? = null)


    @Serializable
    @XmlNamespaceDeclSpec("prefix5=urn:example.org/5;prefix6=urn:example.org/6")
    @XmlSerialName("container", namespace = "urn:example.org", prefix = "")
    data class Container(val child: Child1)

}
