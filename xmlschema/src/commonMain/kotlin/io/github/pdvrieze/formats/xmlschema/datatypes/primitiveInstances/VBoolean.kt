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
import kotlin.jvm.JvmInline

@JvmInline
@Serializable(VBoolean.Companion::class)
value class VBoolean(val value: Boolean): VAnyAtomicType {
    operator fun not(): VBoolean = VBoolean(!value)

    override val xmlString: String get() = value.toString()

    override fun toString(): String = xmlString

    companion object : KSerializer<VBoolean> {
        val TRUE = VBoolean(true)
        val FALSE = VBoolean(false)

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("xmlBoolean", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: VBoolean) = when (value.value) {
            true -> encoder.encodeString("true")
            else -> encoder.encodeString("false")
        }

        override fun deserialize(decoder: Decoder): VBoolean = when (val s = decoder.decodeString()) {
            "0", "false" -> FALSE
            "1", "true" -> TRUE
            else -> throw NumberFormatException("Invalid boolean value: $s")
        }
    }

}
