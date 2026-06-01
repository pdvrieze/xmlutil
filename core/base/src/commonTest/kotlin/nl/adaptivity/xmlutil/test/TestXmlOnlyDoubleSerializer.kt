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
import nl.adaptivity.xmlutil.util.XmlOnlyDoubleSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestXmlOnlyDoubleSerializer {

    @Test
    fun testSerializeDelegatesToEncodeDouble() {
        val enc = ValueCapturingEncoder()
        XmlOnlyDoubleSerializer.serialize(enc, 2.5)
        assertEquals(2.5, enc.capturedDouble)
        assertNull(enc.capturedString)
    }

    @Test
    fun testDeserializeDelegatesToDecodeDouble() {
        assertEquals(2.5, XmlOnlyDoubleSerializer.deserialize(ValueProviderDecoder(2.5)))
    }

    @Test
    fun testSerializeXMLEncodesNormalValueAsString() {
        val enc = ValueCapturingEncoder()
        dummyWriter().use { out -> XmlOnlyDoubleSerializer.serializeXML(enc, out, 1.5, false) }
        assertEquals("1.5", enc.capturedString)
    }

    @Test
    fun testSerializeXMLEncodesNaN() {
        val enc = ValueCapturingEncoder()
        dummyWriter().use { out -> XmlOnlyDoubleSerializer.serializeXML(enc, out, Double.NaN, false) }
        assertEquals("NaN", enc.capturedString)
    }

    @Test
    fun testDeserializeXMLParsesInfinity() {
        val result = XmlOnlyDoubleSerializer.deserializeXML(ValueProviderDecoder("INF"), dummyReader(), null, false)
        assertEquals(Double.POSITIVE_INFINITY, result)
    }

    @Test
    fun testDeserializeXMLParsesNegInfinity() {
        val result = XmlOnlyDoubleSerializer.deserializeXML(ValueProviderDecoder("-INF"), dummyReader(), null, false)
        assertEquals(Double.NEGATIVE_INFINITY, result)
    }

    @Test
    fun testDeserializeXMLParsesNaN() {
        val result = XmlOnlyDoubleSerializer.deserializeXML(ValueProviderDecoder("NaN"), dummyReader(), null, false)
        assertTrue(result.isNaN())
    }
}
