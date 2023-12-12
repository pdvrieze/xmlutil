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

package io.github.pdvrieze.formats.xmlschema.resolved

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
enum class SchemaVersion {
    V1_0, V1_1;

    @OptIn(ExperimentalSerializationApi::class)
    companion object : KSerializer<SchemaVersion?> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
                "io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion",
            PrimitiveKind.STRING
        ).nullable

        override fun serialize(encoder: Encoder, value: SchemaVersion?) {
            when(value) {
                V1_0 -> {
                    encoder.encodeNotNullMark()
                    encoder.encodeString("1.0")
                }
                V1_1 -> {
                    encoder.encodeNotNullMark()
                    encoder.encodeString("1.1")
                }
                null -> encoder.encodeNull()
            }
        }

        override fun deserialize(decoder: Decoder): SchemaVersion? {
            return when {
                decoder.decodeNotNullMark() -> fromXml(decoder.decodeString())
                else -> null
            }
        }

        fun fromXml(xml: String): SchemaVersion? = when (xml) {
            "1.0" -> SchemaVersion.V1_0
            "1.1" -> SchemaVersion.V1_1
            else -> null
        }
    }
}
