/*
 * Copyright (c) 2025.
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

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test that checks whether a primitive value attribute can be written as attribute.
 */
class TestValueAttribute {

    val xml = XML {
        recommended_0_90_2()
    }

    @Test
    fun testSerialize() {
        val data = Container(StringAttr("foo"), IntAttr(42))
        val expected = "<Container str=\"foo\" int=\"42\" />"

        assertXmlEquals(expected, xml.encodeToString(data))
    }

    @Test
    fun testDeserialize() {
        val data = "<Container str=\"foo\" int=\"42\" />"
        val expected = Container(StringAttr("foo"), IntAttr(42))

        assertEquals(expected, xml.decodeFromString<Container>(data))
    }


    @Serializable
    data class Container(
        val str: StringAttr,
        val int: IntAttr
    )

    @JvmInline
    @Serializable
    value class StringAttr(val value: String)

    @JvmInline
    @Serializable
    value class IntAttr(val value: Int)
}
