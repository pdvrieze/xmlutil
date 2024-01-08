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

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VBigDecimalImpl
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VDecimal
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VToken
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(SchemaVersion.Companion::class)
sealed class SchemaVersion : Comparable<SchemaVersion> {

    object V1_0: SchemaVersion() {
        internal val decimal = VBigDecimalImpl("1.0")

        override fun compareTo(other: SchemaVersion): Int = when(other) {
            is UnknownDecimal -> decimal.compareTo(other.ver)
            is UnknownToken -> -1
            V1_0 -> 0
            V1_1 -> -1
        }

        override fun toString(): String = "1.0"
    }

    object V1_1: SchemaVersion() {
        internal val decimal = VBigDecimalImpl("1.1")

        override fun compareTo(other: SchemaVersion): Int = when(other) {
            is UnknownDecimal -> decimal.compareTo(other.ver)
            is UnknownToken -> -1
            V1_0 -> 1
            V1_1 -> 0
        }

        override fun toString(): String = "1.1"

    }

    class UnknownDecimal(val ver: VDecimal): SchemaVersion() {
        override fun compareTo(other: SchemaVersion): Int = when(other) {
            is UnknownDecimal -> ver.compareTo(other.ver)
            is UnknownToken -> -1
            V1_0 -> ver.compareTo(V1_0.decimal)
            V1_1 -> ver.compareTo(V1_1.decimal)
        }

        override fun toString(): String = ver.xmlString
    }

    class UnknownToken(val token: VToken): SchemaVersion() {
        override fun compareTo(other: SchemaVersion): Int = 1

        override fun toString(): String = token.xmlString
    }

    companion object : KSerializer<SchemaVersion> {

        val entries: List<SchemaVersion> = listOf(V1_0, V1_1)

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
                "io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion",
            PrimitiveKind.STRING
        ).nullable

        override fun serialize(encoder: Encoder, value: SchemaVersion) {
            when(value) {
                V1_0 -> encoder.encodeString("1.0")

                V1_1 -> encoder.encodeString("1.1")

                is UnknownDecimal -> encoder.encodeString(value.ver.xmlString)

                is UnknownToken -> encoder.encodeString(value.token.xmlString)
            }
        }

        override fun deserialize(decoder: Decoder): SchemaVersion {
            return fromXml(decoder.decodeString())
        }

        fun fromXml(xml: String): SchemaVersion = when (xml) {
            "1.0" -> V1_0
            "1.1" -> V1_1
            else -> try {
                UnknownDecimal(VBigDecimalImpl(xml))
            } catch (e: NumberFormatException) {
                UnknownToken(VToken(xml))
            }
        }
    }
}
