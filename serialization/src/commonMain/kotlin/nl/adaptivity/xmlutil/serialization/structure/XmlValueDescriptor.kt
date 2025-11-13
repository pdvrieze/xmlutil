/*
 * Copyright (c) 2025.
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

package nl.adaptivity.xmlutil.serialization.structure

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.util.CompactFragment

public sealed class XmlValueDescriptor(
    codecConfig: XML.XmlCodecConfig,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo
) : XmlDescriptor(codecConfig, serializerParent, tagParent) {

    public final override val isCData: Boolean = (serializerParent.useAnnCData
        ?: tagParent.useAnnCData
        ?: serializerParent.elementTypeDescriptor.typeAnnCData) == true


    @OptIn(ExperimentalXmlUtilApi::class)
    public val default: String? = tagParent.useAnnDefault

    private var defaultValue: Any? = UNSET

    internal fun <T> defaultValue(
        xmlCodecBase: XmlCodecBase,
        deserializer: DeserializationStrategy<T>
    ): T {
        defaultValue.let { d ->
            @Suppress("UNCHECKED_CAST")
            if (d != UNSET) return d as T
        }

        @Suppress("UNCHECKED_CAST")
        if (default == null) return null as T

        val ps = DocumentPreserveSpace.DEFAULT.withDefault(defaultPreserveSpace)

        return when {
            effectiveOutputKind.let { it != OutputKind.Text && it != OutputKind.Text } ->
                defaultValue(xmlCodecBase.serializersModule, xmlCodecBase.config, deserializer)

            xmlCodecBase is XmlDecoderBase ->
                deserializer.deserialize(xmlCodecBase.StringDecoder(this, XmlReader.ExtLocationInfo(0, 0, 0), default, ps))

            else -> xmlCodecBase.run {
                val dec = XmlDecoderBase(serializersModule, config, CompactFragment("").getXmlReader())
                    .StringDecoder(this@XmlValueDescriptor, XmlReader.ExtLocationInfo(0, 0, 0), default, ps)

                deserializer.deserialize(dec)
            }
        }
    }

    private fun <T> defaultValue(
        serializersModule: SerializersModule,
        config: XmlConfig,
        deserializer: DeserializationStrategy<T>
    ): T {
        defaultValue.let { d ->
            @Suppress("UNCHECKED_CAST")
            if (d != UNSET) return d as T
        }
        val ps = DocumentPreserveSpace.DEFAULT.withDefault(defaultPreserveSpace)
        val d = when {
            default == null -> null

            effectiveOutputKind.let { it == OutputKind.Attribute || it == OutputKind.Text } -> {
                val xmlDecoderBase =
                    XmlDecoderBase(serializersModule, config, CompactFragment(default).getXmlReader())
                val dec = xmlDecoderBase.StringDecoder(this, XmlReader.ExtLocationInfo(0, 0, 0), default, ps)
                deserializer.deserialize(dec)
            }

            else -> {
                val defaultDecoder =
                    XmlDecoderBase(
                        serializersModule,
                        config,
                        CompactFragment(default).getXmlReader()
                    ).XmlDecoder(this, inheritedPreserveWhitespace = ps)
                deserializer.deserialize(defaultDecoder)
            }
        }
        defaultValue = d
        @Suppress("UNCHECKED_CAST")
        return d as T
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlValueDescriptor

        if (isCData != other.isCData) return false
        if (default != other.default) return false
        return defaultValue == other.defaultValue
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + isCData.hashCode()
        result = 31 * result + (default?.hashCode() ?: 0)
        result = 31 * result + (defaultValue?.hashCode() ?: 0)
        return result
    }

    private object UNSET
}
