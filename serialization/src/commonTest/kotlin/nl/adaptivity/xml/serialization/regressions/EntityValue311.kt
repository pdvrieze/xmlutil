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

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFails

class EntityValue311 {

    @Test
    fun testParseEntity() {
        val xml = "<Test>&amp;</Test>"
        val test = XML.v1.decodeFromString<ValueTest>(xml)
        assertEquals("&", test.value)
    }

    @Test
    fun testParseEntityNoResolve() {
        val xmlReader = xmlStreaming.newGenericReader("<Test>&amp;</Test>", false)
        val test = XML.v1.decodeFromReader<ValueTest>(xmlReader)
        assertEquals("&", test.value)
    }

    @Test
    fun testParseEntityResolve() {
        val xmlReader = xmlStreaming.newGenericReader("<Test>&amp;</Test>", true)
        val test = XML.v1.decodeFromReader<ValueTest>(xmlReader)
        assertEquals("&", test.value)
    }

    @Test
    fun testParseUnknownEntityNoResolve() {
        val xmlReader = xmlStreaming.newGenericReader("<Test>&unknown;</Test>", false)
        val e = assertFails {
            val _ = XML.v1.decodeFromReader<ValueTest>(xmlReader)
        }
        assertContains(e.message?:"", "unknown")
    }

    @Test
    fun testParseUnknownEntityResolve() {
        val xmlReader = xmlStreaming.newGenericReader("<Test>&unknown;</Test>", true)
        val e = assertFails {
            val _ = XML.v1.decodeFromReader<ValueTest>(xmlReader)
        }
        assertContains(e.message ?: "", "unknown")
    }

    @Test
    fun testParseKnownEntityNoResolve() {
        val xmlReader = xmlStreaming.newGenericReader(
            """
                <!DOCTYPE Test [ <!ENTITY internalEntity "Hello, world!"> ] >
                <Test>&internalEntity;</Test>
            """.trimIndent(),
            false
        )
        val test = XML.v1.decodeFromReader<ValueTest>(xmlReader)
        assertEquals("internalEntity", test.value)
    }

    @Test
    fun testParseKnownEntityResolve() {
        val xmlReader = xmlStreaming.newGenericReader(
            """
                <!DOCTYPE Test [ <!ENTITY internalEntity "Hello, world!"> ] >
                <Test>&internalEntity;</Test>
            """.trimIndent(),
            true
        )
        val test = XML.v1.decodeFromReader<ValueTest>(xmlReader)
        assertEquals("Hello, world!", test.value)
    }

    @Serializable
    data class ValueTest(@XmlValue val value: String)
}
