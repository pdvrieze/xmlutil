/*
 * Copyright (c) 2022.
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

package io.github.pdvrieze.xmlutil.testutil

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringReader
import kotlin.jvm.JvmOverloads
import kotlin.test.*

@JvmOverloads
fun assertXmlEquals(expected: String, actual: String, messageProvider: () -> String? = { null }) {
    if (expected != actual) {
        val expectedReader = KtXmlReader(StringReader(expected))
        val actualReader = KtXmlReader(StringReader(actual))

        try {
            assertXmlEquals(expectedReader, actualReader, messageProvider)
        } catch (e: AssertionError) {
            try {
                assertEquals(expected, actual, messageProvider())
            } catch (f: AssertionError) {
                f.addSuppressed(e)
                throw f
            }
        }
    }
}

internal fun XmlReader.nextNotIgnored(): XmlEvent? {
    while (hasNext()) {
        val et = next()
        if (!et.isIgnorable) {
            val ev = toEvent()
            if (! ev.isIgnorable) return toEvent()
        }
    }
    return null
}

@JvmOverloads
fun assertXmlEquals(expected: XmlReader, actual: XmlReader, messageProvider: () -> String? = { null }): Unit {
    val seen = mutableListOf<XmlEvent>()
    do {
        val expEv = expected.nextNotIgnored()
        val actEv = actual.nextNotIgnored()

        when {
            expEv == null -> {
                assertTrue(actEv == null, "${messageProvider()?.let { "$it. " }}Expected nothing, but found $actEv")
                return
            }

            actEv == null -> fail("${messageProvider()?.let { "$it. " }}Expected $expEv, but found nothing")
            else -> assertXmlEquals(expEv, actEv, messageProvider)
        }
    } while (actual.eventType != EventType.END_DOCUMENT && expected.hasNext() && actual.hasNext())

    while (expected.hasNext() && expected.isIgnorable()) {
        expected.next()
    }
    while (actual.hasNext() && actual.isIgnorable()) {
        actual.next()
    }

    assertEquals(expected.hasNext(), actual.hasNext(), messageProvider())
}

@JvmOverloads
fun assertXmlEquals(expectedEvent: XmlEvent, actualEvent: XmlEvent, messageProvider: () -> String? = { null }) {
    assertEquals(expectedEvent.eventType, actualEvent.eventType, "Different event found")
    when (expectedEvent) {
        is XmlEvent.StartElementEvent ->
            assertStartElementEquals(expectedEvent, actualEvent as XmlEvent.StartElementEvent, messageProvider)

        is XmlEvent.EndElementEvent ->
            assertQNameEquivalent(expectedEvent.name, (actualEvent as XmlEvent.EndElementEvent).name, messageProvider)

        is XmlEvent.TextEvent ->
            if (!(expectedEvent.isIgnorable && actualEvent.isIgnorable)) {
                assertEquals(expectedEvent.text, (actualEvent as XmlEvent.TextEvent).text, messageProvider())
            }

        else -> {
            // ignore
        }
    }
}

@JvmOverloads
fun assertQNameEquivalent(expected: QName, actual: QName, messageProvider: () -> String? = { null }) {
    asserter.assertTrue(
        { (messageProvider() ?: "") + "Expected <$expected>, actual <$actual>" },
        expected.isEquivalent(actual)
    )
}

internal fun assertStartElementEquals(
    expectedEvent: XmlEvent.StartElementEvent,
    actualEvent: XmlEvent.StartElementEvent,
    messageProvider: () -> String? = { null }
) {
    assertQNameEquivalent(expectedEvent.name, actualEvent.name, messageProvider)

    val expectedAttributes = expectedEvent.attributes.filter { it.namespaceUri != XMLConstants.XMLNS_ATTRIBUTE_NS_URI }
        .map { XmlEvent.Attribute(it.namespaceUri, it.localName, "", it.value) }
        .sortedBy { "{${it.namespaceUri}}${it.localName}" }
    val actualAttributes = actualEvent.attributes.filter { it.namespaceUri != XMLConstants.XMLNS_ATTRIBUTE_NS_URI }
        .map { XmlEvent.Attribute(it.namespaceUri, it.localName, "", it.value) }
        .sortedBy { "{${it.namespaceUri}}${it.localName}" }

    assertContentEquals(expectedAttributes, actualAttributes, messageProvider())
}
