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
import nl.adaptivity.xmlutil.serialization.CompactFragmentSerializer
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.test.Test
import kotlin.test.assertEquals

class TestCompactFragmentSerializer {
    @Test
    fun testSerializeCompactFragment() {
        val f = FragmentContainer(CompactFragment("""<a><b>"hello"</b></a>"""), "bar")
        val expected = "<FragmentContainer c=\"bar\">${f.fragment.contentString}</FragmentContainer>"
        val actual = XML.Companion.encodeToString(f)
        assertEquals(expected, actual)
    }

    @Test
    fun testDeserializeCompactFragment() {
        val expected = FragmentContainer(CompactFragment("""<a><b>"hello"</b></a>"""), "bar")
        val input = "<FragmentContainer c=\"bar\">${expected.fragment.contentString}</FragmentContainer>"
        val actual = XML.decodeFromString<FragmentContainer>(input)
        assertEquals(expected, actual)
    }

    @Serializable
    data class FragmentContainer(
        @XmlValue(true) @Serializable(CompactFragmentSerializer::class) val fragment: CompactFragment,
        @XmlElement(false)
        val c: String
    )
}
