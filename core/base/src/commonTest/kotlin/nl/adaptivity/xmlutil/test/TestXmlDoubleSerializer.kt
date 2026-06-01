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

import nl.adaptivity.xmlutil.test.util.ValueCapturingEncoder
import nl.adaptivity.xmlutil.test.util.ValueProviderDecoder
import nl.adaptivity.xmlutil.util.XmlDoubleSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestXmlDoubleSerializer {

    @Test
    fun testSerializeNormalValue() {
        val enc = ValueCapturingEncoder()
        XmlDoubleSerializer.serialize(enc, 1.5)
        assertEquals("1.5", enc.capturedString)
    }

    @Test
    fun testSerializeNaN() {
        val enc = ValueCapturingEncoder()
        XmlDoubleSerializer.serialize(enc, Double.NaN)
        assertEquals("NaN", enc.capturedString)
    }

    @Test
    fun testSerializePositiveInfinityAsInf() {
        // POSITIVE_INFINITY > Double.MAX_VALUE → "INF"
        val enc = ValueCapturingEncoder()
        XmlDoubleSerializer.serialize(enc, Double.POSITIVE_INFINITY)
        assertEquals("INF", enc.capturedString)
    }

    @Test
    fun testDeserializeInfAsPositiveInfinity() {
        assertEquals(Double.POSITIVE_INFINITY, XmlDoubleSerializer.deserialize(ValueProviderDecoder("INF")))
    }

    @Test
    fun testDeserializeNegInfAsNegativeInfinity() {
        assertEquals(Double.NEGATIVE_INFINITY, XmlDoubleSerializer.deserialize(ValueProviderDecoder("-INF")))
    }

    @Test
    fun testDeserializeNaN() {
        assertTrue(XmlDoubleSerializer.deserialize(ValueProviderDecoder("NaN")).isNaN())
    }

    @Test
    fun testDeserializeNormalString() {
        assertEquals(3.14, XmlDoubleSerializer.deserialize(ValueProviderDecoder("3.14")))
    }

    @Test
    fun testDeserializeInvalidThrows() {
        assertFailsWith<NumberFormatException> {
            XmlDoubleSerializer.deserialize(ValueProviderDecoder("not-a-number"))
        }
    }

    @Test
    fun testDescriptorSerialName() {
        assertEquals("xmlDouble", XmlDoubleSerializer.descriptor.serialName)
    }
}
