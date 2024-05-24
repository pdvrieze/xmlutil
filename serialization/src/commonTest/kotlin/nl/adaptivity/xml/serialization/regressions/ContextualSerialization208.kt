/*
 * Copyright (c) 2024.
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

package nl.adaptivity.xml.serialization.regressions

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.core.impl.multiplatform.name
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.structure.XmlContextualDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ContextualSerialization208 {
    val xml = XML(
        SerializersModule {
            contextual(AnyIntSerializer)
        }
    ) { recommended_0_87_0() }

    @Test
    fun testSerialization() {
        val data = TestContainer(101, 102)

        val actual = xml.encodeToString(data)
        val expected = "<TestContainer any1=\"101\" any2=\"102\"/>"
        assertEquals(expected, actual)
    }

    @Test
    fun textXmlDescriptor() {
        val data = TestContainer(101, 102)
        val descriptor = xml.xmlDescriptor(TestContainer.serializer()).getElementDescriptor(0)

        assertEquals(QName("TestContainer"), descriptor.tagName)
        assertEquals(2, descriptor.elementsCount)
        val child1Descriptor = assertIs<XmlContextualDescriptor>(descriptor.getElementDescriptor(0))
        val child2Descriptor = assertIs<XmlContextualDescriptor>(descriptor.getElementDescriptor(1))
    }

    object AnyIntSerializer: KSerializer<Any> {
        @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("AnyInt", PrimitiveKind.INT)

        override fun serialize(encoder: Encoder, value: Any) {
            encoder.encodeInt(value as Int)
        }

        override fun deserialize(decoder: Decoder): Any {
            return decoder.decodeInt()
        }
    }

    @Serializable
    class TestContainer(@Contextual val any1: Any, @Contextual val any2: Any)
}
