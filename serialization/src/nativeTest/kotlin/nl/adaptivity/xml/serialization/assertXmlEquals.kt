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

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.XmlEvent.*
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringReader
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

actual fun assertXmlEquals(expected: String, actual: String) {
    if (expected != actual) {
        val expectedReader = KtXmlReader(StringReader(expected)).apply { skipPreamble() }
        val actualReader = KtXmlReader(StringReader(actual)).apply { skipPreamble() }

        try {
            assertXmlEquals(expectedReader, expectedReader)
        } catch (e: AssertionError) {
            try {
                assertEquals(expected, actual)
            } catch (f: AssertionError) {
                f.addSuppressed(e)
                throw f
            }
        }
    }
}

fun XmlReader.skipIgnorable() {
    do { val ev = next() } while (ev.isIgnorable)
}

fun assertXmlEquals(expected: XmlReader, actual: XmlReader): Unit {
    do {
        expected.skipIgnorable()
        actual.skipIgnorable()
        val expectedType = expected.eventType
        val actualType = expected.eventType
        assertEquals(expectedType, actualType, "Different event found")
        assertXmlEquals(expected.toEvent(), actual.toEvent())

    } while (expectedType != EventType.END_DOCUMENT)
}

fun assertXmlEquals(expectedEvent: XmlEvent, actualEvent: XmlEvent) {
    assertEquals(expectedEvent.eventType, actualEvent.eventType, "Different event found")
    when (expectedEvent) {
        is StartElementEvent -> assertStartElementEquals(expectedEvent, actualEvent as StartElementEvent)
        is EndElementEvent -> assertEquals(expectedEvent.name, (actualEvent as EndElementEvent).name)
        is TextEvent -> assertEquals(expectedEvent.text, (actualEvent as TextEvent).text)
    }
}

fun assertStartElementEquals(expectedEvent: StartElementEvent, actualEvent: StartElementEvent) {
    assertEquals(expectedEvent.name, actualEvent.name)
    assertEquals(expectedEvent.attributes.size, actualEvent.attributes.size)
    val expectedAttrs = expectedEvent.attributes.map { Attribute(it.namespaceUri, it.localName, "", it.value) }
        .sortedBy { "{${it.namespaceUri}}${it.localName}" }
    val actualAttrs = actualEvent.attributes.map { Attribute(it.namespaceUri, it.localName, "", it.value) }
        .sortedBy { "{${it.namespaceUri}}${it.localName}" }
    assertContentEquals(expectedAttrs, actualAttrs)
}
