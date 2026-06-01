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
import nl.adaptivity.xmlutil.util.XmlOnlyFloatSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// XmlBooleanSerializer
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// XmlOnlyBooleanSerializer
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// XmlDoubleSerializer
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// XmlOnlyDoubleSerializer
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// XmlFloatSerializer
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// XmlOnlyFloatSerializer
// ---------------------------------------------------------------------------

class TestXmlOnlyFloatSerializer {

    @Test
    fun testSerializeDelegatesToEncodeFloat() {
        val enc = ValueCapturingEncoder()
        XmlOnlyFloatSerializer.serialize(enc, 1.5f)
        assertEquals(1.5f, enc.capturedFloat)
        assertNull(enc.capturedString)
    }

    @Test
    fun testDeserializeDelegatesToDecodeFloat() {
        assertEquals(1.5f, XmlOnlyFloatSerializer.deserialize(ValueProviderDecoder(1.5f)))
    }

    @Test
    fun testSerializeXMLEncodesNormalValueAsString() {
        val enc = ValueCapturingEncoder()
        dummyWriter().use { out -> XmlOnlyFloatSerializer.serializeXML(enc, out, 1.5f, false) }
        assertEquals("1.5", enc.capturedString)
    }

    @Test
    fun testSerializeXMLEncodesNaN() {
        val enc = ValueCapturingEncoder()
        dummyWriter().use { out -> XmlOnlyFloatSerializer.serializeXML(enc, out, Float.NaN, false) }
        assertEquals("NaN", enc.capturedString)
    }

    @Test
    fun testDeserializeXMLParsesInfinity() {
        val result = XmlOnlyFloatSerializer.deserializeXML(ValueProviderDecoder("INF"), dummyReader(), null, false)
        assertEquals(Float.POSITIVE_INFINITY, result)
    }

    @Test
    fun testDeserializeXMLParsesNegInfinity() {
        val result = XmlOnlyFloatSerializer.deserializeXML(ValueProviderDecoder("-INF"), dummyReader(), null, false)
        assertEquals(Float.NEGATIVE_INFINITY, result)
    }

    @Test
    fun testDeserializeXMLParsesNaN() {
        val result = XmlOnlyFloatSerializer.deserializeXML(ValueProviderDecoder("NaN"), dummyReader(), null, false)
        assertTrue(result.isNaN())
    }
}
