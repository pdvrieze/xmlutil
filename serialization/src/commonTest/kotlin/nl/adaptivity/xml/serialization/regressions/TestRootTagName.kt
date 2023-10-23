/*
 * Copyright (c) 2023.
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

package nl.adaptivity.xml.serialization.regressions

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlin.test.*

/**
 * Test that root tags names are handled appropriately. Based on #186.
 */
class TestRootTagName {

    @Test
    fun testParseNoXmlNameAttr() {
        val d: NoXmlName = XML.decodeFromString("<foo data=\"bar\" />")
        assertEquals("bar", d.data)
    }

    @Test
    fun testParseNoXmlNameAttrExplicit() {
        val d: NoXmlName = XML.decodeFromString("<foo data=\"bar\" />", QName("foo"))
        assertEquals("bar", d.data)
    }

    @Test
    fun testParseNoXmlNameAttrExplicitFail() {
        val e = assertFailsWith<XmlException> {
            XML.decodeFromString<NoXmlName>("<foo data=\"bar\" />", QName("not_the_present_tag"))
        }
        val m = assertNotNull(e.message)
        assertEquals("Local name \"foo\" for root tag does not match expected name \"not_the_present_tag\"", m)
    }

    @Test
    fun testParseXmlNameAttr() {
        val e = assertFailsWith<XmlException> {
            val d: XmlName = XML.decodeFromString("<foo data=\"bar\" />")
        }
        val m = assertNotNull(e.message)
        assertEquals("Local name \"foo\" for root tag does not match expected name \"XmlName\"", m)
    }

    @Test
    fun testParseXmlNameAttrExplicit() {
        val d: XmlName = XML.decodeFromString("<foo data=\"bar\" />", QName("foo"))
        assertEquals("bar", d.data)
    }

    @Serializable
    class NoXmlName(val data: String)

    @Serializable
    @XmlSerialName("XmlName", "", "")
    class XmlName(val data: String)
}
