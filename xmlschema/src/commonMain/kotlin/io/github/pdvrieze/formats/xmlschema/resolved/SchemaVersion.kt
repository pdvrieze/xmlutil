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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(SchemaVersion.Companion::class)
sealed class SchemaVersion {

    object V1_0: SchemaVersion()

    object V1_1: SchemaVersion()

    class Unknown(val ver: VDecimal): SchemaVersion()

    @OptIn(ExperimentalSerializationApi::class)
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

                is Unknown -> encoder.encodeString(value.ver.xmlString)
            }
        }

        override fun deserialize(decoder: Decoder): SchemaVersion {
            return fromXml(decoder.decodeString())
        }

        fun fromXml(xml: String): SchemaVersion = when (xml) {
            "1.0" -> V1_0
            "1.1" -> V1_1
            else -> Unknown(VBigDecimalImpl(xml))
        }
    }
}
