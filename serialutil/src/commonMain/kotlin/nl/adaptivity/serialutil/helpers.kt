/*
 * Copyright (c) 2019-2025.
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

package nl.adaptivity.serialutil

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.*
import kotlin.jvm.JvmOverloads

object CharArrayAsStringSerializer : KSerializer<CharArray> {
    override val descriptor = PrimitiveSerialDescriptor("CharArrayAsString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CharArray) =
        encoder.encodeString(value.concatToString())

    override fun deserialize(decoder: Decoder): CharArray = decoder.decodeString().toCharArray()
}

@ExperimentalSerializationApi
fun Decoder.readNullableString(): String? =
    decodeNullableSerializableValue(String.serializer().nullable)

@ExperimentalSerializationApi
@JvmOverloads
fun CompositeDecoder.readNullableString(desc: SerialDescriptor, index: Int, previousValue: String? = null): String? =
    decodeNullableSerializableElement(desc, index, String.serializer().nullable, previousValue)

@ExperimentalSerializationApi
fun CompositeEncoder.encodeNullableStringElement(desc: SerialDescriptor, index: Int, value: String?) =
    encodeNullableSerializableElement(desc, index, String.serializer(), value)

/**
 * Helper function that helps decoding structure elements
 */
inline fun DeserializationStrategy<*>.decodeElements(input: CompositeDecoder, body: (Int) -> Unit) {
    var index = input.decodeElementIndex(descriptor)
    if (index == CompositeDecoder.DECODE_DONE) return

    while (index >= 0) {
        body(index)
        index = input.decodeElementIndex(descriptor)
    }
}

@Deprecated(
    "Use kotlinx.serialization implementation instead",
    ReplaceWith("encodeCollection(desc, collectionSize, body)", "kotlinx.serialization.encoding.encodeCollection")
)
inline fun Encoder.writeCollection(
    desc: SerialDescriptor,
    collectionSize: Int,
    crossinline body: CompositeEncoder.(desc: SerialDescriptor) -> Unit
) {
    encodeCollection(desc, collectionSize) { body(desc) }
}
