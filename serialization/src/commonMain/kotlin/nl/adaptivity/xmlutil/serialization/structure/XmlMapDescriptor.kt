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

import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.OutputKind
import nl.adaptivity.xmlutil.serialization.XML

public class XmlMapDescriptor internal constructor(
    codecConfig: XML.XmlCodecConfig,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
    preserveSpace: TypePreserveSpace
) : XmlListLikeDescriptor(codecConfig, serializerParent, tagParent, preserveSpace) {

    override val outputKind: OutputKind get() = OutputKind.Element

    override val isIdAttr: Boolean get() = false

    public val isValueCollapsed: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
        codecConfig.config.policy.isMapValueCollapsed(serializerParent, valueDescriptor)
    }

    internal val entryName: QName by lazy(LazyThreadSafetyMode.PUBLICATION) {
        when {
            isValueCollapsed -> valueDescriptor.tagName
            else -> codecConfig.config.policy.mapEntryName(serializerParent, isListEluded)
        }
    }

    private val keyDescriptor: XmlDescriptor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val keyNameInfo = codecConfig.config.policy.mapKeyName(serializerParent)
        val parentInfo = ParentInfo(codecConfig.config, this, 0, keyNameInfo)
        val keyTagParent = InjectedParentTag(0, typeDescriptor[0], keyNameInfo, tagParent.namespace)
        from(codecConfig, parentInfo, keyTagParent, canBeAttribute = true)
    }

    private val valueDescriptor: XmlDescriptor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val valueNameInfo = codecConfig.config.policy.mapValueName(serializerParent, isListEluded)
        val parentInfo =
            ParentInfo(codecConfig.config, this, 1, valueNameInfo, if (isListEluded) OutputKind.Element else null)
        val valueTagParent = InjectedParentTag(0, typeDescriptor[1], valueNameInfo, tagParent.namespace)
        from(codecConfig, parentInfo, valueTagParent, canBeAttribute = true)
    }

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        return when (index % 2) {
            0 -> keyDescriptor
            else -> valueDescriptor
        }
    }

    override fun appendTo(builder: Appendable, indent: Int, seen: MutableSet<String>) {
        builder.append(tagName.toString())
            .append(if (isListEluded) ": TransparentMap<" else ": ExplicitMap<")
        getElementDescriptor(0).appendTo(builder, indent + 4, seen)
        builder.append(", ")
        getElementDescriptor(1).appendTo(builder, indent + 4, seen)
        builder.append('>')
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlMapDescriptor

        if (isValueCollapsed != other.isValueCollapsed) return false
        if (entryName != other.entryName) return false
//        if (keyDescriptor != other.keyDescriptor) return false
//        if (valueDescriptor != other.valueDescriptor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + isValueCollapsed.hashCode()
        result = 31 * result + entryName.hashCode()
        result = 31 * result + keyDescriptor.hashCode()
        result = 31 * result + valueDescriptor.hashCode()
        return result
    }


}
