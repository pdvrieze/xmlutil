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
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.*

public class XmlInlineDescriptor internal constructor(
    codecConfig: XML.XmlCodecConfig,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
    canBeAttribute: Boolean,
    @ExperimentalXmlUtilApi
    override val defaultPreserveSpace: TypePreserveSpace
) : XmlValueDescriptor(codecConfig, serializerParent, tagParent) {

    override val isIdAttr: Boolean = serializerParent.useAnnIsId

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = true

    init {
        if (!serializerParent.elementSerialDescriptor.isInline) {
            throw AssertionError("InlineDescriptors are only valid for inline classes")
        }
    }

    override val outputKind: OutputKind get() = child.outputKind//OutputKind.Inline

    /**
     * Use the tag name of the child as the child tagName is already adapted upon this type
     */
    override val tagName: QName
        get() = child.tagName


    @OptIn(ExperimentalSerializationApi::class)
    private val child: XmlDescriptor by lazy(LazyThreadSafetyMode.PUBLICATION) {

        val effectiveUseNameInfo: XmlSerializationPolicy.DeclaredNameInfo = when {
            useNameInfo.annotatedName != null -> useNameInfo

            typeDescriptor.typeNameInfo.annotatedName != null -> typeDescriptor.typeNameInfo

            else -> {
                // This is needed as this descriptor is not complete yet and would use this element's
                // unset name for the namespace.
                val serialName = typeDescriptor.serialDescriptor.getElementName(0)
                val annotation = typeDescriptor.serialDescriptor.getElementAnnotations(0).firstOrNull<XmlSerialName>()
                val qName = annotation?.toQName(serialName, tagParent.namespace)
                val childUseNameInfo =
                    XmlSerializationPolicy.DeclaredNameInfo(
                        serialName,
                        qName,
                        annotation?.namespace == UNSET_ANNOTATION_VALUE
                    )

                when {
                    childUseNameInfo.annotatedName != null -> childUseNameInfo

                    else -> useNameInfo
                }

            }
        }

        val useParentInfo = ParentInfo(codecConfig.config, this, 0, effectiveUseNameInfo, serializerParent.elementUseOutputKind)

        from(codecConfig, useParentInfo, tagParent, canBeAttribute)
    }

    override val isUnsigned: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        serialDescriptor in UNSIGNED_SERIALIZER_DESCRIPTORS || child.isUnsigned
    }

    override val visibleDescendantOrSelf: XmlDescriptor
        get() = child.visibleDescendantOrSelf

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        if (index != 0) throw IllegalArgumentException("Inline classes only have one child")
        return child
    }

    override fun appendTo(builder: Appendable, indent: Int, seen: MutableSet<String>) {
        builder.apply {
            append(tagName.toString())
            append(": Inline (")
            child.toString(this, indent + 4, seen)
            append(')')
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlInlineDescriptor

        if (isIdAttr != other.isIdAttr) return false
        if (defaultPreserveSpace != other.defaultPreserveSpace) return false
        if (isUnsigned != other.isUnsigned) return false
        if (child.tagName != other.tagName) return false
        if (child.typeDescriptor != other.child.typeDescriptor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + isIdAttr.hashCode()
        result = 31 * result + defaultPreserveSpace.hashCode()
        result = 31 * result + isUnsigned.hashCode()
        result = 31 * result + child.hashCode()
        return result
    }

    private companion object {
        val UNSIGNED_SERIALIZER_DESCRIPTORS: Array<SerialDescriptor> = arrayOf(
            UByte.Companion.serializer().descriptor,
            UShort.serializer().descriptor,
            UInt.serializer().descriptor,
            ULong.serializer().descriptor
        )
    }
}
