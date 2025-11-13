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
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.capturedKClass
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.OutputKind
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.reflect.KClass

public class XmlContextualDescriptor @ExperimentalXmlUtilApi
internal constructor(
    codecConfig: XML.XmlCodecConfig,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
    private val canBeAttribute: Boolean,
    @ExperimentalXmlUtilApi
    override val defaultPreserveSpace: TypePreserveSpace,
) : XmlDescriptor(codecConfig, serializerParent, tagParent) {
    @ExperimentalSerializationApi
    override val doInline: Boolean get() = false

    override val isIdAttr: Boolean get() = false

    override val elementsCount: Int get() = 0

    @OptIn(ExperimentalSerializationApi::class)
    public val context: KClass<*>? = serializerParent.elementSerialDescriptor.capturedKClass

    override val effectiveOutputKind: OutputKind get() = outputKind

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
