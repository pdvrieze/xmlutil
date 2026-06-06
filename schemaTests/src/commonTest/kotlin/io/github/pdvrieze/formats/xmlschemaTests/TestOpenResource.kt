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

package io.github.pdvrieze.formats.xmlschemaTests

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.core.internal.StringInOutBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class TestOpenResource {
    @Test
    fun testOpenResource() {
        val r = getResource("/override.xsd")
        r.withXmlReader {
            assertEquals(EventType.START_DOCUMENT, it.next())
        }
    }

    @Test
    fun testParseResourceAsText() {
        val r = getResource("/XMLSchema.xsd")
        val t = r.getText()

        var eventCounts: MutableMap<EventType, Counter> = mutableMapOf()
        repeat (100) {
            eventCounts = mutableMapOf<EventType, Counter>()
            KtXmlReader(StringInOutBuffer(t)).use {
                while (it.hasNext()) {
                    val event = it.next()
                    if (event == EventType.TEXT) {
                        val _ = it.text
                    }
                    eventCounts.getOrPut(event) { Counter(0) }.count++
                }
            }
        }
    }

    @Test
    fun testParseResource() {
        val r = getResource("/XMLSchema.xsd")
        var eventCounts: MutableMap<EventType, Counter> = mutableMapOf()
        repeat (100) {
            eventCounts = mutableMapOf<EventType, Counter>()
            r.withXmlReader(true) {
                while (it.hasNext()) {
                    val event = it.next()
                    if (event == EventType.TEXT) {
                        val _ = it.text
                    }
                    eventCounts.getOrPut(event) { Counter(0) }.count++
                }
            }
        }
        assertEquals(1, eventCounts[EventType.START_DOCUMENT]?.count)
        assertEquals(1, eventCounts[EventType.END_DOCUMENT]?.count)
        assertEquals(null, eventCounts[EventType.PROCESSING_INSTRUCTION]?.count)
        assertEquals(1, eventCounts[EventType.DOCDECL]?.count)
        assertEquals(1027, eventCounts[EventType.START_ELEMENT]?.count, "Count of START_ELEMENT not expected")
        assertEquals(1027, eventCounts[EventType.END_ELEMENT]?.count, "Count of END_ELEMENT not expected")
        assertEquals(68, eventCounts[EventType.TEXT]?.count, "Count of TEXT not expected")
        assertEquals(1567, eventCounts[EventType.IGNORABLE_WHITESPACE]?.count, "Count of IGNORABLE_WHITESPACE not expected")
        assertEquals(null, eventCounts[EventType.CDSECT]?.count, "Count of CDSECT not expected")
        assertEquals(1, eventCounts[EventType.COMMENT]?.count, "Count of COMMENT not expected")
    }

    data class Counter(var count: Int)
}
