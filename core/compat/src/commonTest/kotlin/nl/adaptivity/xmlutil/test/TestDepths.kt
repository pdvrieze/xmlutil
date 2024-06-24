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

package nl.adaptivity.xmlutil.test

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.isXmlWhitespace
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class TestDepths {

    val input = """<?xml version="1.0" encoding="UTF-8"?>
        |<?dummy ?>
        |<outer>
        |  <inner>
        |    <a>depth 3</a>
        |  </inner>
        |  <inner2>Depth 2</inner2>
        |</outer>
        |<!-- after outer -->
    """.trimMargin()

    @Test
    fun testDepthGeneric() {
        val reader = xmlStreaming.newGenericReader(input)
        testDepth(reader)
    }

    @Test
    fun testDepthPlatform() {
        val reader = xmlStreaming.newReader(input)
        testDepth(reader)
    }

    private fun testDepth(reader: XmlReader) {
        assertEquals(0, reader.depth)

        assertEquals(EventType.START_DOCUMENT, reader.next())
        assertEquals(0, reader.depth)

        assertEquals(EventType.PROCESSING_INSTRUCTION, reader.nextNonWS(0))
        assertEquals(0, reader.depth)

        assertEquals(EventType.START_ELEMENT, reader.nextNonWS(0))
        assertEquals("outer", reader.localName)
        assertEquals(1, reader.depth)

        assertContains(listOf(EventType.TEXT, EventType.IGNORABLE_WHITESPACE), reader.next())
        assertEquals(1, reader.depth)

        assertEquals(EventType.START_ELEMENT, reader.next())
        assertEquals("inner", reader.localName)
        assertEquals(2, reader.depth)

        assertContains(listOf(EventType.TEXT, EventType.IGNORABLE_WHITESPACE), reader.next())
        assertEquals(2, reader.depth)

        assertEquals(EventType.START_ELEMENT, reader.next())
        assertEquals("a", reader.localName)
        assertEquals(3, reader.depth)

        assertEquals(EventType.TEXT, reader.next())
        assertEquals("depth 3", reader.text)
        assertEquals(3, reader.depth)

        assertEquals(EventType.END_ELEMENT, reader.next())
        assertEquals("a", reader.localName)
        assertEquals(3, reader.depth)

        assertEquals(EventType.END_ELEMENT, reader.nextNonWS(2))
        assertEquals("inner", reader.localName)
        assertEquals(2, reader.depth)

        assertEquals(EventType.START_ELEMENT, reader.nextNonWS(1))
        assertEquals("inner2", reader.localName)
        assertEquals(2, reader.depth)

        assertEquals(EventType.TEXT, reader.next())
        assertEquals("Depth 2", reader.text)
        assertEquals(2, reader.depth)

        assertEquals(EventType.END_ELEMENT, reader.next())
        assertEquals("inner2", reader.localName)
        assertEquals(2, reader.depth)

        assertEquals(EventType.END_ELEMENT, reader.nextNonWS(1))
        assertEquals("outer", reader.localName)
        assertEquals(1, reader.depth)

        assertEquals(EventType.COMMENT, reader.nextNonWS(0))
        assertEquals(" after outer ", reader.text)

        assertEquals(EventType.END_DOCUMENT, reader.nextNonWS(0))
    }

    private fun XmlReader.nextNonWS(expectedDepth: Int): EventType {
        val n = next()
        return when (n) {
            EventType.IGNORABLE_WHITESPACE -> {
                assertEquals(expectedDepth, depth)
                nextNonWS(expectedDepth)
            }

            EventType.TEXT -> {
                assertEquals(expectedDepth, depth)
                when {
                    isXmlWhitespace(text) -> nextNonWS(expectedDepth)
                    else -> n
                }
            }

            else -> n
        }
    }
}
