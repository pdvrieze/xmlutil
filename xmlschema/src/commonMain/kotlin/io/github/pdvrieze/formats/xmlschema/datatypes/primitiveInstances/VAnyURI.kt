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

import io.github.pdvrieze.xml.schematypes.values.XsdAnyURI
import io.github.pdvrieze.xml.schematypes.values.toAnyUri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.xmlCollapseWhitespace

typealias VAnyURI = XsdAnyURI

@Serializable(VAnyURIXX.Serializer::class)
sealed class VAnyURIXX : VAnyAtomicType, CharSequence {

    val value: String get() = xmlString

    operator fun component1(): String = value

    companion object Serializer : KSerializer<VAnyURIXX> {
        operator fun invoke(value: String) = value.toAnyUriXX()
        operator fun invoke(value: CharSequence) = value.toString().toAnyUriXX()
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("xsd.anyURI", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): VAnyURIXX {
            return decoder.decodeString().toAnyUriXX()
        }


        override fun serialize(encoder: Encoder, value: VAnyURIXX) {
            encoder.encodeString(value.xmlString)
        }

        val EMPTY: VAnyURIXX = "".toAnyUriXX()

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VAnyURIXX) return false

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

@XmlUtilInternal
private fun String.toAnyUriXX(): VAnyURIXX {
    val s = xmlCollapseWhitespace(this)
    return kotlin.runCatching { VParsedURI(s) }.getOrElse { VRelaxedURI(s) }
}

@XmlUtilInternal
fun String.toAnyUri(): XsdAnyURI = toAnyUri()
