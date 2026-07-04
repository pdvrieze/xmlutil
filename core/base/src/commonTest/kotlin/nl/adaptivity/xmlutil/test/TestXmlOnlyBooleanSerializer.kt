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

package nl.adaptivity.xmlutil.test

import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.test.util.ValueCapturingEncoder
import nl.adaptivity.xmlutil.test.util.ValueProviderDecoder
import nl.adaptivity.xmlutil.test.util.dummyReader
import nl.adaptivity.xmlutil.test.util.dummyWriter
import nl.adaptivity.xmlutil.util.XmlOnlyBooleanSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TestXmlOnlyBooleanSerializer {

    @Test
    fun testSerializeDelegatesToEncodeBoolean() {
        val enc = ValueCapturingEncoder()
        XmlOnlyBooleanSerializer.serialize(enc, true)
        assertEquals(true, enc.capturedBoolean)
        assertNull(enc.capturedString, "XML-only serializer must not encode as string in non-XML path")
    }

    @Test
    fun testDeserializeDelegatesToDecodeBoolean() {
        assertEquals(false, XmlOnlyBooleanSerializer.deserialize(ValueProviderDecoder(false)))
    }

    @Test
    fun testSerializeXMLEncodesAsString() {
        val enc = ValueCapturingEncoder()
        dummyWriter().use { out -> XmlOnlyBooleanSerializer.serializeXML(enc, out, true, false) }
        assertEquals("true", enc.capturedString)
    }

    @Test
    fun testSerializeXMLEncodesAsFalseString() {
        val enc = ValueCapturingEncoder()
        dummyWriter().use { out -> XmlOnlyBooleanSerializer.serializeXML(enc, out, false, false) }
        assertEquals("false", enc.capturedString)
    }

    @Test
    fun testDeserializeXMLParsesTrueString() {
        val dec = ValueProviderDecoder("true")
        val _ = dummyReader().use { r -> XmlOnlyBooleanSerializer.deserializeXML(dec, r, null, false) }
        // result is true from decoder; just assert no exception and correctness
        assertEquals(
            true, XmlOnlyBooleanSerializer.deserializeXML(
                ValueProviderDecoder("true"),
                dummyReader(), null, false
            )
        )
    }

    @Test
    fun testDeserializeXMLParsesFalseString() {
        assertEquals(
            false, XmlOnlyBooleanSerializer.deserializeXML(
                ValueProviderDecoder("false"),
                dummyReader(), null, false
            )
        )
    }

    @Test
    fun testDeserializeXMLParsesOneAsTrue() {
        assertEquals(
            true,
            XmlOnlyBooleanSerializer.deserializeXML(ValueProviderDecoder("1"), dummyReader(), null, false)
        )
    }

    @Test
    fun testDeserializeXMLInvalidThrows() {
        assertFailsWith<NumberFormatException> {
            XmlOnlyBooleanSerializer.deserializeXML(ValueProviderDecoder("bad"), dummyReader(), null, false)
        }
    }
}
