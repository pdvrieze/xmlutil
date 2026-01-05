/*
 * Copyright (c) 2021-2026.
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

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.IDType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class VID private constructor(override val xmlString: String) : VNCName {

    init {
        IDType.mdlFacets.validateValue(this)
    }

    override fun toString(): String = xmlString

    companion object : KSerializer<VID> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
            "io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID",
            PrimitiveKind.STRING
        )

        override fun serialize(encoder: Encoder, value: VID) {
            encoder.encodeString(value.xmlString)
        }

        override fun deserialize(decoder: Decoder): VID {
            return invoke(decoder.decodeString())
        }

        operator fun invoke(rawId: String): VID {
            return VID(
                IDType.mdlFacets.validateRepresentationOnly(
                    IDType,
                    VString(rawId)
                ).xmlString
            )
        }

    }

}


