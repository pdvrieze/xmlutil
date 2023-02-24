/*
 * Copyright (c) 2023.
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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.ChunkedDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlValue
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class TestChunkedDecoding {
    @Test
    fun decodeChunked() {
        val rnd = Random(514321L)
        val textContent = buildString {
            for (i in 0..40000/*0000*/) {
                append(CHARACTERS.random(rnd))
            }
        }

        val xmlString = "<TextContainer>$textContent</TextContainer>"
        val decoded = XML.decodeFromString(TextContainer.serializer(), xmlString)

        assertEquals(textContent, decoded.text)
    }

    companion object {
        val CHARACTERS = (('A'..'Z') + ('a'..'z') + ' ').toCharArray()
    }

    @Serializable
    class TextContainer(
        @Serializable(ChunkedTextDecoder::class)
        @XmlValue(true) val text: String
    )

    object ChunkedTextDecoder: KSerializer<String> {
        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("ChunkedString", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): String {
            require(decoder is ChunkedDecoder)
            return buildString {
                decoder.decodeStringChunked {
                    require(it.length<=16384)
                    append(it)
                }
            }
        }

        override fun serialize(encoder: Encoder, value: String) {
            TODO("not implemented")
        }

    }
}
