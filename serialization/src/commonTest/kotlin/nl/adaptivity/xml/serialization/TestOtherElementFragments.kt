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

package nl.adaptivity.xml.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.CompactFragmentSerializer
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.test.Test
import kotlin.test.assertEquals

class TestOtherElementFragments {
    @Test
    fun testSerializeCompactFragment() {
        val f = Container(listOf(CompactFragment("""<a><b>"hello"</b></a>"""), CompactFragment("<foo>xx</foo>")), "bar")
        val expected = "<Container>${f.children[0].contentString}${f.children[1].contentString}<c>bar</c></Container>"
        val actual = XML { autoPolymorphic = true }.encodeToString(f)
        assertEquals(expected, actual)
    }

    @Test
    fun testDeserializeCompactFragment() {
        val expected =
            Container(listOf(CompactFragment("""<a><b>"hello"</b></a>"""), CompactFragment("<foo>xx</foo>")), "bar")
        val input =
            "<Container>${expected.children[0].contentString}<c>bar</c>${expected.children[1].contentString}</Container>"
        val actual = XML { autoPolymorphic = true }.decodeFromString<Container>(input)
        assertEquals(expected, actual)
    }

    @Serializable
    data class Container(
        @XmlValue(true) val children: List<@Serializable(CompactFragmentSerializer::class) CompactFragment>,
        @XmlElement(true)
        val c: String
    )
}
