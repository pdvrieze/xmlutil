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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.OutputKind
import nl.adaptivity.xmlutil.serialization.XML

public class XmlPrimitiveDescriptor : XmlValueDescriptor {

    @ExperimentalXmlUtilApi
    internal constructor(
        codecConfig: XML.XmlCodecConfig,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        canBeAttribute: Boolean,
        defaultPreserveSpace: TypePreserveSpace
    ) : super(codecConfig, serializerParent, tagParent) {
        this.defaultPreserveSpace = defaultPreserveSpace
        this.outputKind = codecConfig.config.policy.effectiveOutputKind(serializerParent, tagParent, canBeAttribute)
    }

    private constructor(
        original: XmlPrimitiveDescriptor,
        serializerParent: SafeParentInfo = original.serializerParent,
        tagParent: SafeParentInfo = original.tagParent,
        overriddenSerializer: KSerializer<*>? = original.overriddenSerializer,
        typeDescriptor: XmlTypeDescriptor = original.typeDescriptor,
        namespaceDecls: List<Namespace> = original.namespaceDecls,
        tagNameProvider: XmlDescriptor.() -> Lazy<QName> = { original._tagName },
        decoderPropertiesProvider: XmlDescriptor.() -> Lazy<DecoderProperties> = { original._decoderProperties },
        isCData: Boolean = original.isCData,
        default: String? = original.default,
        defaultPreserveSpace: TypePreserveSpace = original.defaultPreserveSpace,
        outputKind: OutputKind = original.outputKind,
    ) : super(
        original,
        serializerParent,
        tagParent,
        overriddenSerializer,
        typeDescriptor,
        namespaceDecls,
        tagNameProvider,
        decoderPropertiesProvider,
        isCData,
        default
    ) {
        this.defaultPreserveSpace = defaultPreserveSpace
        this.outputKind = outputKind
    }


    @ExperimentalXmlUtilApi
    override val defaultPreserveSpace: TypePreserveSpace

    override val isIdAttr: Boolean get() = serializerParent.useAnnIsId

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = false

    override val outputKind: OutputKind

    override val elementsCount: Int get() = 0

    @OptIn(ExperimentalSerializationApi::class)
    override fun appendTo(builder: Appendable, indent: Int, seen: MutableSet<String>) {
        when {
            _tagName.isInitialized() -> builder.append(_tagName.value.toString())
            else -> builder.append("<tagname pending>")
        }.append(':')
            .append(kind.toString())
            .append(" = ")
            .append(outputKind.toString())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlPrimitiveDescriptor

        if (isIdAttr != other.isIdAttr) return false
        if (defaultPreserveSpace != other.defaultPreserveSpace) return false
        if (outputKind != other.outputKind) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + isIdAttr.hashCode()
        result = 31 * result + defaultPreserveSpace.hashCode()
        result = 31 * result + outputKind.hashCode()
        return result
    }

}
