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
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.capturedKClass
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.OutputKind
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy
import kotlin.reflect.KClass

public class XmlContextualDescriptor : XmlDescriptor {

    @OptIn(ExperimentalSerializationApi::class)
    public val context: KClass<*>?

    private val canBeAttribute: Boolean

    @ExperimentalXmlUtilApi
    override val defaultPreserveSpace: TypePreserveSpace

    @ExperimentalXmlUtilApi
    internal constructor(
        codecConfig: XML.XmlCodecConfig,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        canBeAttribute: Boolean,
        defaultPreserveSpace: TypePreserveSpace
    ) : super(codecConfig, serializerParent, tagParent) {
        this.context = serializerParent.elementSerialDescriptor.capturedKClass
        this.canBeAttribute = canBeAttribute
        this.defaultPreserveSpace = defaultPreserveSpace
    }

    private constructor(
        original: XmlContextualDescriptor,
        serializerParent: SafeParentInfo = original.serializerParent,
        tagParent: SafeParentInfo = original.tagParent,
        overriddenSerializer: KSerializer<*>? = original.overriddenSerializer,
        useNameInfo: XmlSerializationPolicy.DeclaredNameInfo = original.useNameInfo,
        typeDescriptor: XmlTypeDescriptor = original.typeDescriptor,
        namespaceDecls: List<Namespace> = original.namespaceDecls,
        tagNameProvider: XmlDescriptor.() -> Lazy<QName> = { original._tagName },
        decoderPropertiesProvider: XmlDescriptor.() -> Lazy<DecoderProperties> = { original._decoderProperties },
        context: KClass<*>? = original.context,
        canBeAttribute: Boolean = original.canBeAttribute,
        defaultPreserveSpace: TypePreserveSpace = original.defaultPreserveSpace,
    ) : super(
        original,
        serializerParent,
        tagParent,
        overriddenSerializer,
        useNameInfo,
        typeDescriptor,
        namespaceDecls,
        tagNameProvider,
        decoderPropertiesProvider
    ) {
        this.context = context
        this.canBeAttribute = canBeAttribute
        this.defaultPreserveSpace = defaultPreserveSpace
    }

    override fun copy(nameProvider: XmlDescriptor.() -> Lazy<QName>): XmlContextualDescriptor {
        return XmlContextualDescriptor(this, tagNameProvider = nameProvider)
    }

    @ExperimentalSerializationApi
    override val doInline: Boolean get() = false

    override val effectiveOutputKind: OutputKind get() = outputKind

    override val isIdAttr: Boolean get() = false

    override val elementsCount: Int get() = 0

    override fun appendTo(builder: Appendable, indent: Int, seen: MutableSet<String>) {
        builder
            .append("CONTEXTUAL(")
            .append(tagParent.elementUseNameInfo.run { annotatedName?.toString() ?: serialName })
            .append(")")
    }

    internal fun resolve(
        codecConfig: XML.XmlCodecConfig,
        descriptor: SerialDescriptor
    ): XmlDescriptor {
        val typeDescriptor = codecConfig.config.lookupTypeDesc(tagParent.namespace, descriptor)

        val overriddenParentInfo = DetachedParent(tagParent.namespace, typeDescriptor, useNameInfo)

        return from(codecConfig, overriddenParentInfo, tagParent, canBeAttribute)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlContextualDescriptor

        if (canBeAttribute != other.canBeAttribute) return false
        if (defaultPreserveSpace != other.defaultPreserveSpace) return false
        if (context != other.context) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + canBeAttribute.hashCode()
        result = 31 * result + defaultPreserveSpace.hashCode()
        result = 31 * result + (context?.hashCode() ?: 0)
        return result
    }

    override val outputKind: OutputKind get() = OutputKind.Inline
}
