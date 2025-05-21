/*
 * Copyright (c) 2025.
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

package net.devrieze.serialization.examples.boxingTag

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.*
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/*
 * This is an example for implementing #80 in a more compact way.
 */

@Serializable
class Header(
    @XmlSerialName("ModelInfo") val modelInfo: BoxedString,
    @XmlSerialName("ModelVersion") val modelVersion: BoxedString,
    @XmlSerialName("PersistencyVersion") val persistencyVersion: BoxedString,
)

fun main() {
    val data = Header("foo", "bar", "baz")

    println(XML { recommended_0_91_0() }.encodeToString(data))
}

typealias BoxedString = @Serializable(BoxedSerializer::class) String

object BoxedSerializer: KSerializer<String> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(BoxedSerializer::class.qualifiedName!!) {
        element("value", String.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value)
        }
    }

    override fun deserialize(decoder: Decoder): String {
        return decoder.decodeStructure(descriptor) {
            lateinit var value: String
            do {
                when (val i = this.decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break
                    0 -> value = decodeStringElement(descriptor, 0)
                    else -> throw IllegalArgumentException("Unknown index $i")
                }
            } while (true)
            value
        }
    }
}
