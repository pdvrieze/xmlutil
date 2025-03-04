/*
 * Copyright (c) 2024.
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

package net.devrieze.xmlutil.serialization.kxio.test

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.io.Buffer
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.Serializable
import net.devrieze.xmlutil.serialization.kxio.decodeFromSource
import net.devrieze.xmlutil.serialization.kxio.encodeToSink
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlValue
import kotlin.test.Test
import kotlin.test.assertEquals

class IOTest {

    private val xml = XML { recommended_0_90_2()
        xmlDeclMode = XmlDeclMode.None
    }

    @Test
    fun testSerialize() {
        val expected = "<SimpleData>foo</SimpleData>"
        val buffer = Buffer()
        xml.encodeToSink(buffer, SimpleData("foo"))
        assertXmlEquals(expected, buffer.readString())
    }

    @Test
    fun testDeserialize() {
        val expected = SimpleData("bar")
        val source = Buffer().apply { writeString("<SimpleData>bar</SimpleData>") }
        assertEquals(28, source.size)

        val actual = xml.decodeFromSource<SimpleData>(source)
        assertEquals(expected, actual)
    }

    @Serializable
    data class SimpleData(@XmlValue val content: String)
}

