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

package nl.adaptivity.xmlutil.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlSerializer
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.xml

public typealias XmlFloat = @Serializable(XmlOnlyFloatSerializer::class) Float

/**
 * Serializer for float values that parses according to the XMLSchema standard. This version
 * works for all formats.
 */
public object XmlFloatSerializer : KSerializer<Float> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("xmlFloat", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Float): Unit = when {
        value.isNaN() -> encoder.encodeString("NaN")
        value > Float.MAX_VALUE -> encoder.encodeString("INF")
        value < Float.MIN_VALUE -> encoder.encodeString("INF")
        else -> encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Float = when (val s = decoder.decodeString()) {
        "INF" -> Float.POSITIVE_INFINITY
        "-INF" -> Float.NEGATIVE_INFINITY
        "NaN" -> Float.NaN
/*
        "Infinity" -> throw NumberFormatException("Java Infinitity is not valid as XML")
        "-Infinity" -> throw NumberFormatException("Java -Infinitity is not valid as XML")
*/
        else -> s.toFloat()
    }
}

/**
 * Serializer for floats that serializes floats according to XML for the XML format, but regularly
 * for non-Xml formats.
 */
public object XmlOnlyFloatSerializer : XmlSerializer<Float> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("xmlOnlyFloat", PrimitiveKind.FLOAT)
        .xml(PrimitiveSerialDescriptor("xmlFloat", PrimitiveKind.STRING))

    override fun serialize(encoder: Encoder, value: Float) {
        encoder.encodeFloat(value)
    }

    override fun serializeXML(
        encoder: Encoder,
        output: XmlWriter,
        value: Float,
        isValueChild: Boolean
    ) {
        // we ignore the output as we don't know how the string should be serialized
        when {
            value.isNaN() -> encoder.encodeString("NaN")
            value > Float.MAX_VALUE -> encoder.encodeString("INF")
            value < Float.MIN_VALUE -> encoder.encodeString("INF")
            else -> encoder.encodeString(value.toString())
        }
    }

    override fun deserialize(decoder: Decoder): Float {
        return decoder.decodeFloat()
    }

    override fun deserializeXML(
        decoder: Decoder,
        input: XmlReader,
        previousValue: Float?,
        isValueChild: Boolean
    ): Float {
        return when (val s = decoder.decodeString()) {
            "INF" -> Float.POSITIVE_INFINITY
            "-INF" -> Float.NEGATIVE_INFINITY
            "NaN" -> Float.NaN
            else -> s.toFloat()
        }
    }
}
