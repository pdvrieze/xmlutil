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

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.xmlCollapseWhitespace

@Serializable(VAnyURI.Serializer::class)
sealed class VAnyURI : VAnyAtomicType, CharSequence {

    val value: String get() = xmlString

    operator fun component1(): String = value

    companion object Serializer : KSerializer<VAnyURI> {
        operator fun invoke(value: String) = value.toAnyUri()
        operator fun invoke(value: CharSequence) = value.toString().toAnyUri()
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("xsd.anyURI", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): VAnyURI {
            return decoder.decodeString().toAnyUri()
        }


        override fun serialize(encoder: Encoder, value: VAnyURI) {
            encoder.encodeString(value.xmlString)
        }

        val EMPTY: VAnyURI = "".toAnyUri()

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VAnyURI) return false

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

@XmlUtilInternal
fun String.toAnyUri(): VAnyURI {
    val s = xmlCollapseWhitespace(this)
    return kotlin.runCatching { VParsedURI(s) }.getOrElse { VRelaxedURI(s) }
}
