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
import nl.adaptivity.xmlutil.util.XmlBooleanSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestXmlBooleanSerializer {

    @Test
    fun testSerializeTrueEncodesTrue() {
        val enc = ValueCapturingEncoder()
        XmlBooleanSerializer.serialize(enc, true)
        assertEquals("true", enc.capturedString)
    }

    @Test
    fun testSerializeFalseEncodesFalse() {
        val enc = ValueCapturingEncoder()
        XmlBooleanSerializer.serialize(enc, false)
        assertEquals("false", enc.capturedString)
    }

    @Test
    fun testDeserializeTrueString() {
        assertEquals(true, XmlBooleanSerializer.deserialize(ValueProviderDecoder("true")))
    }

    @Test
    fun testDeserializeFalseString() {
        assertEquals(false, XmlBooleanSerializer.deserialize(ValueProviderDecoder("false")))
    }

    @Test
    fun testDeserializeOneAsTrue() {
        assertEquals(true, XmlBooleanSerializer.deserialize(ValueProviderDecoder("1")))
    }

    @Test
    fun testDeserializeZeroAsFalse() {
        assertEquals(false, XmlBooleanSerializer.deserialize(ValueProviderDecoder("0")))
    }

    @Test
    fun testDeserializeInvalidThrowsNumberFormatException() {
        assertFailsWith<NumberFormatException> {
            XmlBooleanSerializer.deserialize(ValueProviderDecoder("yes"))
        }
    }

    @Test
    fun testDescriptorSerialName() {
        assertEquals("xmlBoolean", XmlBooleanSerializer.descriptor.serialName)
    }
}
