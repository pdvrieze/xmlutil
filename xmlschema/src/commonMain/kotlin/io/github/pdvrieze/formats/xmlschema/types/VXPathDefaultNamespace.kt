/*
 * Copyright (c) 2023-2026.
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

package io.github.pdvrieze.formats.xmlschema.types

import io.github.pdvrieze.xml.schematypes.types.AnyURIType
import io.github.pdvrieze.xml.schematypes.types.StringType
import io.github.pdvrieze.xml.schematypes.values.XsdAnyURI
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import io.github.pdvrieze.xml.schematypes.values.XsdString
import io.github.pdvrieze.xml.schematypes.values.toAnyUri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(VXPathDefaultNamespace.Serializer::class)
sealed class VXPathDefaultNamespace(): XsdAtomic {


    object DEFAULTNAMESPACE: VXPathDefaultNamespace(), XsdString {
        override val xmlString: String = "##defaultNamespace"
        override val schemaType: StringType<XsdString> get() = StringType.Instance
    }

    object TARGETNAMESPACE: VXPathDefaultNamespace(), XsdString {
        override val xmlString: String = "##targetNamespace"
        override val schemaType: StringType<XsdString> get() = StringType.Instance
    }

    object LOCAL: VXPathDefaultNamespace(), XsdString {
        override val xmlString: String = "##local"
        override val schemaType: StringType<XsdString> get() = StringType.Instance
    }

    class Uri(val uri: XsdAnyURI): VXPathDefaultNamespace(), XsdAnyURI {
        override val schemaType: AnyURIType<XsdAnyURI> get() = uri.schemaType
        override val xmlString: String get() = uri.xmlString
        override val length: Int get() = uri.length
        override fun get(index: Int): Char = uri.get(index)

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
            uri.subSequence(startIndex, endIndex)
    }

    companion object Serializer: KSerializer<VXPathDefaultNamespace> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("xpathDefaultNamespace", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: VXPathDefaultNamespace) =
            encoder.encodeString(value.xmlString)

        override fun deserialize(decoder: Decoder): VXPathDefaultNamespace = when (val str = decoder.decodeString()){
            "##defaultNamespace" -> DEFAULTNAMESPACE
            "##targetNamespace" -> TARGETNAMESPACE
            "##local" -> LOCAL
            else -> Uri(str.toAnyUri())
        }
    }
}
