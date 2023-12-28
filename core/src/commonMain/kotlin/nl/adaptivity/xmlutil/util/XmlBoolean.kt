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

public typealias XmlBoolean = @Serializable(XmlOnlyBooleanSerializer::class) Boolean

/**
 * Serializer for boolean values that parses according to the XMLSchema standard. This means
 * that only the values: `0`, `1`, `false`, and `true` are accepted. This version applies this
 * rule independent of format
 */
public object XmlBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("xmlBoolean", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Boolean): Unit = when (value) {
        true -> encoder.encodeString("true")
        else -> encoder.encodeString("false")
    }

    override fun deserialize(decoder: Decoder): Boolean = when (val s = decoder.decodeString()) {
        "0", "false" -> false
        "1", "true" -> true
        else -> throw NumberFormatException("Invalid boolean value: $s")
    }
}

public object XmlOnlyBooleanSerializer : XmlSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("xmlOnlyBoolean", PrimitiveKind.BOOLEAN)
        .xml(PrimitiveSerialDescriptor("xmlBoolean", PrimitiveKind.STRING))

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }

    override fun serializeXML(encoder: Encoder, output: XmlWriter, value: Boolean, isValueChild: Boolean) {
        // we ignore the output as we don't know how the string should be serialized
        when (value) {
            true -> encoder.encodeString("true")
            else -> encoder.encodeString("false")
        }
    }

    override fun deserialize(decoder: Decoder): Boolean {
        return decoder.decodeBoolean()
    }

    override fun deserializeXML(
        decoder: Decoder,
        input: XmlReader,
        previousValue: Boolean?,
        isValueChild: Boolean
    ): Boolean {
        return when (val s = decoder.decodeString()) {
            "0", "false" -> false
            "1", "true" -> true
            else -> throw NumberFormatException("Invalid boolean value: $s")
        }
    }
}
