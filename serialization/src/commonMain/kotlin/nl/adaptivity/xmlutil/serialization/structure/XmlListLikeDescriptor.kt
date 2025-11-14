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
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy

public sealed class XmlListLikeDescriptor : XmlDescriptor {

    @ExperimentalXmlUtilApi
    final override val defaultPreserveSpace: TypePreserveSpace

    public val isListEluded: Boolean

    protected constructor(
        codecConfig: XML.XmlCodecConfig,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo = serializerParent,
        defaultPreserveSpace: TypePreserveSpace
    ) : super(codecConfig, serializerParent, tagParent) {
        this.defaultPreserveSpace = defaultPreserveSpace
        this.isListEluded = when {
            tagParent is DetachedParent && tagParent.isDocumentRoot -> false
            else -> codecConfig.config.policy.isListEluded(serializerParent, tagParent)
        }
    }

    protected constructor(
        original: XmlListLikeDescriptor,
        serializerParent: SafeParentInfo = original.serializerParent,
        tagParent: SafeParentInfo = original.tagParent,
        overriddenSerializer: KSerializer<*>? = original.overriddenSerializer,
        useNameInfo: XmlSerializationPolicy.DeclaredNameInfo = original.useNameInfo,
        typeDescriptor: XmlTypeDescriptor = original.typeDescriptor,
        namespaceDecls: List<Namespace> = original.namespaceDecls,
        tagNameProvider: XmlDescriptor.() -> Lazy<QName> = { _tagName },
        decoderPropertiesProvider: XmlDescriptor.() -> Lazy<DecoderProperties> = { _decoderProperties },
        defaultPreserveSpace: TypePreserveSpace = original.defaultPreserveSpace,
        isListEluded: Boolean = original.isListEluded,
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
        this.defaultPreserveSpace = defaultPreserveSpace
        this.isListEluded = isListEluded
    }

    @ExperimentalSerializationApi
    final override val doInline: Boolean get() = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlListLikeDescriptor

        if (isListEluded != other.isListEluded) return false
        if (defaultPreserveSpace != other.defaultPreserveSpace) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + isListEluded.hashCode()
        result = 31 * result + defaultPreserveSpace.hashCode()
        return result
    }

}
