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

package io.github.pdvrieze.xml.schematypes.values

import io.github.pdvrieze.xml.schematypes.Collation
import io.github.pdvrieze.xml.schematypes.types.BooleanType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdBooleanImpl
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.xmlTrimWhitespace

@Serializable(XsdBoolean.Companion::class)
@ExperimentalXmlUtilApi
interface XsdBoolean : XsdPrimitive {
    override val schemaType: BooleanType<XsdBoolean>
    val value: Boolean

    override fun compareTo(other: XsdPrimitive, collation: Collation): Int {
        require(other is XsdBoolean) { "Cannot compare $this with $other" }
        return (if(value) 1 else 0).compareTo(if(other.value) 1 else 0)
    }

    companion object : KSerializer<XsdBoolean> {
        val TRUE: XsdBoolean = XsdBooleanImpl(true)
        val FALSE: XsdBoolean = XsdBooleanImpl(false)

        operator fun invoke(value: Boolean): XsdBoolean = if (value) TRUE else FALSE

        public operator fun invoke(str: CharSequence): XsdBoolean {
            return when (val s = xmlTrimWhitespace(str)) {
                "0", "false" -> FALSE

                "1", "true" -> TRUE

                else -> throw NumberFormatException("Invalid boolean value: $s")
            }
        }

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("xsd.boolean", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: XsdBoolean) = when (value.value) {
            true -> encoder.encodeString("true")
            else -> encoder.encodeString("false")
        }

        override fun deserialize(decoder: Decoder): XsdBoolean {
            return invoke(decoder.decodeString())
        }
    }

}

