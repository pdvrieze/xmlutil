/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
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

fun CompositeEncoder.writeNullableStringElementValue(desc: SerialDescriptor, index: Int, value: String?) = encodeNullableSerializableElement(desc, index, StringSerializer, value)


inline fun DeserializationStrategy<*>.readElements(input: CompositeDecoder, body: (Int) -> Unit) {
    var elem = input.decodeElementIndex(descriptor)
    while (elem >= 0) {
        body(elem)
        elem = input.decodeElementIndex(descriptor)
    }
}

inline fun <T> Decoder.readBegin(desc: SerialDescriptor, body: CompositeDecoder.(desc: SerialDescriptor) -> T):T {
    val input = beginStructure(desc)
    return input.body(desc).also {
        input.endStructure(desc)
    }
}

inline fun Encoder.writeStructure(desc: SerialDescriptor, body: CompositeEncoder.(desc: SerialDescriptor) -> Unit) {
    val output = beginStructure(desc)
    try {
        output.body(desc)
    } finally {
        output.endStructure(desc)
    }
}