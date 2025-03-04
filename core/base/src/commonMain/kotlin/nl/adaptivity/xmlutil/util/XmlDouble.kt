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

public typealias XmlDouble = @Serializable(XmlOnlyDoubleSerializer::class) Double

/**
 * Serializer for double values that parses according to the XMLSchema standard. This version
 * works for all formats.
 */
public object XmlDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("xmlDouble", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Double): Unit = when {
        value.isNaN() -> encoder.encodeString("NaN")
        value > Double.MAX_VALUE -> encoder.encodeString("INF")
        value < Double.MIN_VALUE -> encoder.encodeString("INF")
        else -> encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Double = when (val s = decoder.decodeString()) {
        "INF" -> Double.POSITIVE_INFINITY
        "-INF" -> Double.NEGATIVE_INFINITY
        "NaN" -> Double.NaN
/*
        "Infinity" -> throw NumberFormatException("Java Infinitity is not valid as XML")
        "-Infinity" -> throw NumberFormatException("Java -Infinitity is not valid as XML")
*/
        else -> s.toDouble()
    }
}

/**
 * Serializer for doubles that serializes doubles according to XML for the XML format, but regularly
 * for non-Xml formats.
 */
public object XmlOnlyDoubleSerializer : XmlSerializer<Double> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("xmlOnlyDouble", PrimitiveKind.DOUBLE)
        .xml(PrimitiveSerialDescriptor("xmlDouble", PrimitiveKind.STRING))

    override fun serialize(encoder: Encoder, value: Double) {
        encoder.encodeDouble(value)
    }

    override fun serializeXML(
        encoder: Encoder,
        output: XmlWriter,
        value: Double,
        isValueChild: Boolean
    ) {
        // we ignore the output as we don't know how the string should be serialized
        when {
            value.isNaN() -> encoder.encodeString("NaN")
            value > Double.MAX_VALUE -> encoder.encodeString("INF")
            value < Double.MIN_VALUE -> encoder.encodeString("INF")
            else -> encoder.encodeString(value.toString())
        }
    }

    override fun deserialize(decoder: Decoder): Double {
        return decoder.decodeDouble()
    }

    override fun deserializeXML(
        decoder: Decoder,
        input: XmlReader,
        previousValue: Double?,
        isValueChild: Boolean
    ): Double {
        return when (val s = decoder.decodeString()) {
            "INF" -> Double.POSITIVE_INFINITY
            "-INF" -> Double.NEGATIVE_INFINITY
            "NaN" -> Double.NaN
            else -> s.toDouble()
        }
    }
}
