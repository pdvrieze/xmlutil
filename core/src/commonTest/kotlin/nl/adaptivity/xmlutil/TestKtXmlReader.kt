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

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestKtXmlReader {

    @Test
    fun testReadSingleTag() {
        val xml = "<MixedAttributeContainer xmlns:a=\"a\" xmlns:e=\"c\" attr1=\"value1\" a:b=\"dyn1\" e:d=\"dyn2\" attr2=\"value2\"/>"

        KtXmlReader(StringReader(xml)).use { reader ->
            assertTrue(reader.hasNext())
            assertEquals(EventType.START_DOCUMENT, reader.next())

            assertTrue(reader.hasNext())
            assertEquals(EventType.START_ELEMENT, reader.next())
            assertEquals(QName("MixedAttributeContainer"), reader.name)
            assertEquals(4, reader.attributeCount)

            assertTrue(reader.hasNext())
            assertEquals(EventType.END_ELEMENT, reader.next())

            assertTrue(reader.hasNext())
            assertEquals(EventType.END_DOCUMENT, reader.next(), "Expected end of document, location: ${reader.locationInfo}")

            assertFalse(reader.hasNext())

        }
    }

    @Test
    fun testGenericReadEntity() {
        val xml = """<tag>&lt;foo&amp;&#039;&gt;</tag>"""
        XmlStreaming.newGenericReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals(QName("tag"), reader.name)

            val actualText = StringBuilder()
            while (reader.next().isTextElement) {
                actualText.append(reader.text)
            }

            assertEquals("<foo&'>", actualText.toString())

            assertEquals(EventType.END_ELEMENT, reader.eventType)
            assertEquals(QName("tag"), reader.name)

            assertEquals(EventType.END_DOCUMENT, reader.next())
        }
    }

    @Test
    fun testReadEntity() {
        val xml = """<tag>&lt;foo&amp;&#039;&gt;</tag>"""
        XmlStreaming.newReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals(QName("tag"), reader.name)

            val actualText = StringBuilder()
            while (reader.next().isTextElement) {
                actualText.append(reader.text)
            }

            assertEquals("<foo&'>", actualText.toString())

            assertEquals(EventType.END_ELEMENT, reader.eventType)
            assertEquals(QName("tag"), reader.name)

            assertEquals(EventType.END_DOCUMENT, reader.next())
        }
    }
}
