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
import nl.adaptivity.xmlutil.util.XmlFloatSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestXmlFloatSerializer {

    @Test
    fun testSerializeNormalValue() {
        val enc = ValueCapturingEncoder()
        XmlFloatSerializer.serialize(enc, 1.5f)
        assertEquals("1.5", enc.capturedString)
    }

    @Test
    fun testSerializeNaN() {
        val enc = ValueCapturingEncoder()
        XmlFloatSerializer.serialize(enc, Float.NaN)
        assertEquals("NaN", enc.capturedString)
    }

    @Test
    fun testSerializePositiveInfinityAsInf() {
        // POSITIVE_INFINITY > Float.MAX_VALUE → "INF"
        val enc = ValueCapturingEncoder()
        XmlFloatSerializer.serialize(enc, Float.POSITIVE_INFINITY)
        assertEquals("INF", enc.capturedString)
    }

    @Test
    fun testDeserializeInfAsPositiveInfinity() {
        assertEquals(Float.POSITIVE_INFINITY, XmlFloatSerializer.deserialize(ValueProviderDecoder("INF")))
    }

    @Test
    fun testDeserializeNegInfAsNegativeInfinity() {
        assertEquals(Float.NEGATIVE_INFINITY, XmlFloatSerializer.deserialize(ValueProviderDecoder("-INF")))
    }

    @Test
    fun testDeserializeNaN() {
        assertTrue(XmlFloatSerializer.deserialize(ValueProviderDecoder("NaN")).isNaN())
    }

    @Test
    fun testDeserializeNormalString() {
        assertEquals(1.5f, XmlFloatSerializer.deserialize(ValueProviderDecoder("1.5")))
    }

    @Test
    fun testDeserializeInvalidThrows() {
        assertFailsWith<NumberFormatException> {
            XmlFloatSerializer.deserialize(ValueProviderDecoder("bad"))
        }
    }

    @Test
    fun testDescriptorSerialName() {
        assertEquals("xmlFloat", XmlFloatSerializer.descriptor.serialName)
    }
}
