/*
 * Copyright (c) 2021.
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

package io.github.pdvrieze.formats.xmlschema.types

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(VXPathDefaultNamespace.Serializer::class)
sealed class VXPathDefaultNamespace {

    object DEFAULTNAMESPACE: VXPathDefaultNamespace()
    object TARGETNAMESPACE: VXPathDefaultNamespace()
    object LOCAL: VXPathDefaultNamespace()
    class Uri(val value: VAnyURI): VXPathDefaultNamespace()

    companion object Serializer: KSerializer<VXPathDefaultNamespace> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("xpathDefaultNamespace", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: VXPathDefaultNamespace) = encoder.encodeString(when (value) {
            DEFAULTNAMESPACE -> "##defaultNamespace"
            TARGETNAMESPACE -> "##targetNamespace"
            LOCAL -> "##local"
            is Uri -> value.value.value
        })

        override fun deserialize(decoder: Decoder): VXPathDefaultNamespace = when (val str = decoder.decodeString()){
            "##defaultNamespace" -> DEFAULTNAMESPACE
            "##targetNamespace" -> TARGETNAMESPACE
            "##local" -> LOCAL
            else -> Uri(VAnyURI(str))
        }
    }
}
