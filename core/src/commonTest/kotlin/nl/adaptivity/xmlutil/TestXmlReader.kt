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

package nl.adaptivity.xmlutil

import kotlin.test.Test
import kotlin.test.assertEquals

class TestXmlReader {

    @Test
    fun testNamespaceDecls() {
        val reader = XmlStreaming.newReader("""
            <root xmlns="foo"><ns2:bar xmlns:ns2="bar"/></root>
        """.trimIndent())

        run {
            var event = reader.next()
            if (event == EventType.START_DOCUMENT) event = reader.next()
            assertEquals(EventType.START_ELEMENT, event)

        }
        assertEquals(listOf(XmlEvent.NamespaceImpl("", "foo")),reader.namespaceDecls)

        assertEquals(EventType.START_ELEMENT, reader.next())
        assertEquals(listOf(XmlEvent.NamespaceImpl("ns2", "bar")),reader.namespaceDecls)

        assertEquals(EventType.END_ELEMENT, reader.next())
        assertEquals(EventType.END_ELEMENT, reader.next())
    }
}
