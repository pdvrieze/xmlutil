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

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.qname
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlChildrenName
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * Test based upon/taken from #159
 */
class ListOfNullableEntriesWithNilAttr {

    val format = XML {
        nilAttribute = qname("http://www.w3.org/2001/XMLSchema-instance", "nil", "xsi") to "true"

    }

    val expected = "<Root><ExpiryDateTimeArray><ExpiryDateTime xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:nil=\"true\"/></ExpiryDateTimeArray></Root>"

    @Serializable
    @XmlSerialName("Root")
    class TestRoot(
        @XmlElement
        @XmlSerialName("ExpiryDateTimeArray", "", "")
        @XmlChildrenName("ExpiryDateTime", "", "")
        val values: List<String?>
    )

    @Test
    fun testEncode() {
        val xml = format.encodeToString(TestRoot(listOf(null)))
        assertXmlEquals(expected, xml)
    }

    @Test
    fun testDecode() {
        val testObject = format.decodeFromString<TestRoot>(expected)
        assertContentEquals(listOf(null), testObject.values)
    }


}
