/*
 * Copyright (c) 2026.
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

package io.github.pdvrieze.xmlutil.testutil

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML

@OptIn(ExperimentalSerializationApi::class)
object LocationInfoSerializer: KSerializer<XmlReader.ExtLocationInfo?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("nl.adaptivity.xmlutil.XmlReader.ExtLocationInfo", PrimitiveKind.STRING).nullable


    override fun serialize(encoder: Encoder, value: XmlReader.ExtLocationInfo?) {
        encoder.encodeNull()
    }

    override fun deserialize(decoder: Decoder): XmlReader.ExtLocationInfo? {
        return when (decoder) {
            is XML.XmlInput -> decoder.input.extLocationInfo as? XmlReader.ExtLocationInfo?
            else -> null
        }
    }
}

typealias SerLocationInfo = @Serializable(with = LocationInfoSerializer::class) XmlReader.ExtLocationInfo?
