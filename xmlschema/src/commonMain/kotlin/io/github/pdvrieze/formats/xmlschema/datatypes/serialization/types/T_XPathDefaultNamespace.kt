/*
 * Copyright (c) 2021.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(T_XPathDefaultNamespace.Serializer::class)
sealed class T_XPathDefaultNamespace {

    object DEFAULTNAMESPACE: T_XPathDefaultNamespace()
    object TARGETNAMESPACE: T_XPathDefaultNamespace()
    object LOCAL: T_XPathDefaultNamespace()
    class Uri(val value: VAnyURI): T_XPathDefaultNamespace()

    companion object Serializer: KSerializer<T_XPathDefaultNamespace> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("xpathDefaultNamespace", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: T_XPathDefaultNamespace) = encoder.encodeString(when (value) {
            DEFAULTNAMESPACE -> "##defaultNamespace"
            TARGETNAMESPACE -> "##targetNamespace"
            LOCAL -> "##local"
            is Uri -> value.value.value
        })

        override fun deserialize(decoder: Decoder): T_XPathDefaultNamespace = when (val str = decoder.decodeString()){
            "##defaultNamespace" -> DEFAULTNAMESPACE
            "##targetNamespace" -> TARGETNAMESPACE
            "##local" -> LOCAL
            else -> Uri(VAnyURI(str))
        }
    }
}
