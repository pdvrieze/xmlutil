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
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.*

public class XmlInlineDescriptor : XmlValueDescriptor {

    internal constructor(
        codecConfig: XML.XmlCodecConfig,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        canBeAttribute: Boolean,
        defaultPreserveSpace: TypePreserveSpace
    ) : super(codecConfig, serializerParent, tagParent) {
        if (!serializerParent.elementSerialDescriptor.isInline) {
            throw AssertionError("InlineDescriptors are only valid for inline classes")
        }

        this.defaultPreserveSpace = defaultPreserveSpace

        lazyData = lazy(LazyThreadSafetyMode.PUBLICATION) {
            LazyData(codecConfig, this, canBeAttribute)
        }
    }

    private constructor(
        original: XmlInlineDescriptor,
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
        default,
    ) {
        this.defaultPreserveSpace = defaultPreserveSpace
        this.lazyData = original.lazyData
    }


    override fun copy(nameProvider: XmlDescriptor.() -> Lazy<QName>): XmlInlineDescriptor {
        return XmlInlineDescriptor(this, tagNameProvider = nameProvider)
    }

    @ExperimentalXmlUtilApi
    public override val defaultPreserveSpace: TypePreserveSpace

    override val isIdAttr: Boolean = serializerParent.useAnnIsId

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = true

    override val outputKind: OutputKind get() = child.outputKind//OutputKind.Inline

    /**
     * Use the tag name of the child as the child tagName is already adapted upon this type
     */
    override val tagName: QName
        get() = child.tagName


    private val lazyData: Lazy<LazyData>

    @OptIn(ExperimentalSerializationApi::class)
    private val child: XmlDescriptor get() = lazyData.value.child

    override val isUnsigned: Boolean get() = lazyData.value.isUnsigned

    override val visibleDescendantOrSelf: XmlDescriptor
        get() = child.visibleDescendantOrSelf

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        require(index == 0) { "Inline classes only have one child" }
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

    private class LazyData {
        val child: XmlDescriptor
        val isUnsigned: Boolean

        constructor(codecConfig: XML.XmlCodecConfig, parent: XmlInlineDescriptor, canBeAttribute: Boolean) {
            val useNameInfo = parent.useNameInfo
            val typeDescriptor = parent.typeDescriptor
            val tagParent = parent.tagParent

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

            val useParentInfo = ParentInfo(codecConfig.config, parent, 0, effectiveUseNameInfo,
                parent.serializerParent.elementUseOutputKind)

            child = from(codecConfig, useParentInfo, tagParent, canBeAttribute)

            isUnsigned = parent.serialDescriptor in UNSIGNED_SERIALIZER_DESCRIPTORS || child.isUnsigned
        }
    }

    private companion object {
        val UNSIGNED_SERIALIZER_DESCRIPTORS: Array<SerialDescriptor> = arrayOf(
            UByte.serializer().descriptor,
            UShort.serializer().descriptor,
            UInt.serializer().descriptor,
            ULong.serializer().descriptor
        )
    }
}
