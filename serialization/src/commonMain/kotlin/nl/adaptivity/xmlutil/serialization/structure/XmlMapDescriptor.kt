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

import kotlinx.serialization.KSerializer
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.OutputKind
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy

public class XmlMapDescriptor : XmlListLikeDescriptor {

    internal constructor(
        codecConfig: XML.XmlCodecConfig,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        preserveSpace: TypePreserveSpace
    ) : super(codecConfig, serializerParent, tagParent, preserveSpace) {
        descriptors = lazy { ChildDescs(codecConfig, this) }
    }

    private constructor(
        original: XmlMapDescriptor,
        serializerParent: SafeParentInfo = original.serializerParent,
        tagParent: SafeParentInfo = original.tagParent,
        overriddenSerializer: KSerializer<*>? = original.overriddenSerializer,
        useNameInfo: XmlSerializationPolicy.DeclaredNameInfo = original.useNameInfo,
        typeDescriptor: XmlTypeDescriptor = original.typeDescriptor,
        namespaceDecls: List<Namespace> = original.namespaceDecls,
        tagNameProvider: XmlDescriptor.() -> Lazy<QName> = { original._tagName },
        decoderPropertiesProvider: XmlDescriptor.() -> Lazy<DecoderProperties> = { original._decoderProperties },
        defaultPreserveSpace: TypePreserveSpace = original.defaultPreserveSpace,
        isListEluded: Boolean = original.isListEluded,
        descriptors: Lazy<ChildDescs> = original.descriptors,
    ) : super(
        original,
        serializerParent,
        tagParent,
        overriddenSerializer,
        useNameInfo,
        typeDescriptor,
        namespaceDecls,
        tagNameProvider,
        decoderPropertiesProvider,
        defaultPreserveSpace,
        isListEluded
    ) {
        this.descriptors = descriptors
    }

    override fun copy(nameProvider: XmlDescriptor.() -> Lazy<QName>): XmlMapDescriptor {
        return XmlMapDescriptor(this, tagNameProvider = nameProvider)
    }

    override val outputKind: OutputKind get() = OutputKind.Element

    override val isIdAttr: Boolean get() = false

    private val descriptors: Lazy<ChildDescs>

    public val isValueCollapsed: Boolean get() = descriptors.value.isValueCollapsed

    internal val entryName: QName get() = descriptors.value.entryName

    private val keyDescriptor: XmlDescriptor get() = descriptors.value.keyDescriptor

    private val valueDescriptor: XmlDescriptor get() = descriptors.value.valueDescriptor

    override val visibleDescendantOrSelf: XmlDescriptor
        get() = when {
            isListEluded && isValueCollapsed -> valueDescriptor.visibleDescendantOrSelf
            else -> this
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


    private class ChildDescs(
        val keyDescriptor: XmlDescriptor,
        val valueDescriptor: XmlDescriptor,
        val isValueCollapsed: Boolean,
        val entryName: QName,
    ) {

        constructor(codecConfig: XML.XmlCodecConfig, parent: XmlMapDescriptor): this(
            codecConfig = codecConfig,
            parent = parent,
            keyDescriptor = parent.run {
                val keyNameInfo = codecConfig.config.policy.mapKeyName(serializerParent)
                val parentInfo = ParentInfo(codecConfig.config, parent, 0, keyNameInfo)
                val keyTagParent = InjectedParentTag(0, typeDescriptor[0], keyNameInfo, tagParent.namespace)
                from(codecConfig, parentInfo, keyTagParent, canBeAttribute = true)
            },
            valueDescriptor = parent.run {
                val valueNameInfo = codecConfig.config.policy.mapValueName(serializerParent, isListEluded)
                val parentInfo = ParentInfo(codecConfig.config, parent, 1, valueNameInfo, if (isListEluded) OutputKind.Element else null)
                val valueTagParent = InjectedParentTag(0, typeDescriptor[1], valueNameInfo, tagParent.namespace)
                from(codecConfig, parentInfo, valueTagParent, canBeAttribute = true)
            },
        )

        constructor(codecConfig: XML.XmlCodecConfig, parent: XmlMapDescriptor, keyDescriptor: XmlDescriptor, valueDescriptor: XmlDescriptor): this(
            codecConfig = codecConfig,
            parent = parent,
            keyDescriptor = keyDescriptor,
            valueDescriptor = valueDescriptor,
            isValueCollapsed = codecConfig.config.policy.isMapValueCollapsed(parent.serializerParent, valueDescriptor),
        )

        constructor(codecConfig: XML.XmlCodecConfig, parent: XmlMapDescriptor, keyDescriptor: XmlDescriptor, valueDescriptor: XmlDescriptor, isValueCollapsed: Boolean): this(
            keyDescriptor = keyDescriptor,
            valueDescriptor = valueDescriptor,
            isValueCollapsed = isValueCollapsed,
            entryName = when {
                isValueCollapsed -> valueDescriptor.tagName
                else -> codecConfig.config.policy.mapEntryName(parent.serializerParent, parent.isListEluded)
            }
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ChildDescs

            if (isValueCollapsed != other.isValueCollapsed) return false
            if (keyDescriptor != other.keyDescriptor) return false
            if (valueDescriptor != other.valueDescriptor) return false
            if (entryName != other.entryName) return false

            return true
        }

        override fun hashCode(): Int {
            var result = isValueCollapsed.hashCode()
            result = 31 * result + keyDescriptor.hashCode()
            result = 31 * result + valueDescriptor.hashCode()
            result = 31 * result + entryName.hashCode()
            return result
        }

    }

}
