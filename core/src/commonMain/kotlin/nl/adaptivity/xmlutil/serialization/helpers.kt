/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringSerializer
import nl.adaptivity.xmlutil.impl.CharArraySequence
import nl.adaptivity.xmlutil.multiplatform.toCharArray

object CharArrayAsStringSerializer : KSerializer<CharArray> {
    override val descriptor = simpleSerialClassDesc<CharArray>()

    override fun serialize(encoder: Encoder, obj: CharArray) = encoder.encodeString(
        CharArraySequence(obj).toString())

    override fun deserialize(decoder: Decoder): CharArray = decoder.decodeString().toCharArray()

    override fun patch(decoder: Decoder, old: CharArray): CharArray = deserialize(decoder)
}

@Suppress("UNCHECKED_CAST")
fun Decoder.readNullableString(): String? = decodeNullableSerializableValue(StringSerializer as DeserializationStrategy<String?>)

@Suppress("UNCHECKED_CAST")
fun CompositeDecoder.readNullableString(desc: SerialDescriptor, index: Int): String? = decodeNullableSerializableElement(desc, index, StringSerializer as DeserializationStrategy<String?>)

@Deprecated("Use new name", ReplaceWith("encodeNullableStringElement(desc, index, value)"))
fun CompositeEncoder.writeNullableStringElementValue(desc: SerialDescriptor, index: Int, value: String?) = encodeNullableStringElement(desc, index, value)

fun CompositeEncoder.encodeNullableStringElement(desc: SerialDescriptor, index: Int, value: String?) = encodeNullableSerializableElement(desc, index, StringSerializer, value)

@Deprecated("Use newer name", ReplaceWith("decodeElements(input, body)"))
inline fun DeserializationStrategy<*>.readElements(input: CompositeDecoder, body: (Int) -> Unit) =
    decodeElements(input, body)

/**
 * Helper function that helps decoding structure elements
 */
inline fun DeserializationStrategy<*>.decodeElements(input: CompositeDecoder, body: (Int) -> Unit) {
    var index = input.decodeElementIndex(descriptor)
    when (index) {
        CompositeDecoder.READ_DONE -> return
        CompositeDecoder.READ_ALL -> {
            for( elem in 0 until descriptor.elementsCount) {
                body(elem)
            }
            return
        }
    }
    while (index >= 0) {
        body(index)
        index = input.decodeElementIndex(descriptor)
    }
}

@Deprecated("Use new named version that matches the newer api", ReplaceWith("decodeStructure(desc, body)"))
inline fun <T> Decoder.readBegin(desc: SerialDescriptor, body: CompositeDecoder.(desc: SerialDescriptor) -> T):T =
    decodeStructure(desc, body)


/**
 * Helper function that automatically closes the decoder on close.
 */
inline fun <T> Decoder.decodeStructure(desc: SerialDescriptor, body: CompositeDecoder.(desc: SerialDescriptor) -> T):T {
    val input = beginStructure(desc)
    var skipEnd = false
    try {
        return input.body(desc)
    } catch (e: Exception) {
        skipEnd = true
        throw e
    } finally {
        if (! skipEnd) {
            input.endStructure((desc))
        }
    }
}

inline fun Encoder.writeStructure(desc: SerialDescriptor, body: CompositeEncoder.(desc: SerialDescriptor) -> Unit) {
    val output = beginStructure(desc)
    var skipEnd = false
    try {
        return output.body(desc)
    } catch (e: Exception) {
        skipEnd = true
        throw e
    } finally {
        if (! skipEnd) {
            output.endStructure(desc)
        }
    }
}
