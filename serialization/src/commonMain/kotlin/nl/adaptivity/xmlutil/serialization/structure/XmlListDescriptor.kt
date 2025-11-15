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
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.toNamespace

public class XmlListDescriptor : XmlListLikeDescriptor {

    override val outputKind: OutputKind

    public val delimiters: Array<String>

    private val _childDescriptor: Lazy<XmlDescriptor>
    private val childDescriptor: XmlDescriptor get() = _childDescriptor.value

    internal constructor(
        codecConfig: XML.XmlCodecConfig,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        preserveSpace: TypePreserveSpace
    ) : super(codecConfig, serializerParent, tagParent, preserveSpace) {
        @OptIn(ExperimentalSerializationApi::class)
        outputKind = when {
            tagParent.useAnnIsElement == false ->
                OutputKind.Attribute

            tagParent.useAnnIsId -> OutputKind.Attribute

            !isListEluded -> OutputKind.Element

            tagParent.useAnnIsValue == true -> {
                val namespace = tagParent.namespace
                val childTypeDescriptor =
                    codecConfig.config.lookupTypeDesc(namespace, serialDescriptor.getElementDescriptor(0))

                when (childTypeDescriptor.serialDescriptor.kind) {
                    is PolymorphicKind -> when {
                        codecConfig.config.policy.isTransparentPolymorphic(
                            DetachedParent(
                                namespace, childTypeDescriptor,
                                XmlSerializationPolicy.DeclaredNameInfo("item")
                            ),
                            tagParent
                        ) -> OutputKind.Mixed

                        else -> OutputKind.Element
                    }

                    SerialKind.ENUM,
                    StructureKind.OBJECT,
                    is PrimitiveKind -> OutputKind.Text

                    else -> OutputKind.Mixed
                }
            }


            else -> OutputKind.Element
        }
        @OptIn(ExperimentalXmlUtilApi::class)
        delimiters = when (outputKind) {
            OutputKind.Attribute ->
                codecConfig.config.policy.attributeListDelimiters(
                    ParentInfo(codecConfig.config, this, 0, useNameInfo, outputKind),
                    tagParent
                )

            OutputKind.Text ->
                codecConfig.config.policy.textListDelimiters(
                    ParentInfo(codecConfig.config, this, 0, useNameInfo, outputKind),
                    tagParent
                )

            else -> emptyArray()
        }

        _childDescriptor = lazy(LazyThreadSafetyMode.PUBLICATION) {
            val childrenNameAnnotation = tagParent.useAnnChildrenName

            val useNameInfo = when {
                childrenNameAnnotation != null -> XmlSerializationPolicy.DeclaredNameInfo(
                    childrenNameAnnotation.value,
                    childrenNameAnnotation.toQName(tagName.toNamespace()),
                    childrenNameAnnotation.namespace == UNSET_ANNOTATION_VALUE
                )

                !isListEluded -> null // if we have a list, don't repeat the outer name (at least allow the policy to decide)

                else -> tagParent.elementUseNameInfo
            }

            from(
                codecConfig,
                ParentInfo(codecConfig.config, this, 0, useNameInfo, outputKind),
                tagParent,
                canBeAttribute = false
            )
        }
    }

    private constructor(
        original: XmlListDescriptor,
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
        outputKind: OutputKind = original.outputKind,
        delimiters: Array<String> = original.delimiters,
        childDescriptorProvider: Lazy<XmlDescriptor> = original._childDescriptor,
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
        this.outputKind = outputKind
        this.delimiters = delimiters
        this._childDescriptor = childDescriptorProvider
    }

    override fun copy(nameProvider: XmlDescriptor.() -> Lazy<QName>): XmlListDescriptor {
        return XmlListDescriptor(this, tagNameProvider = nameProvider)
    }

    override val isIdAttr: Boolean get() = false

    override val visibleDescendantOrSelf: XmlDescriptor
        get() = when {
            isListEluded -> childDescriptor.visibleDescendantOrSelf
            else -> this
        }

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        return childDescriptor
    }

    override fun appendTo(builder: Appendable, indent: Int, seen: MutableSet<String>) {
        builder.apply {
            append(tagName.toString())
            when {
                isListEluded -> {
                    append(": EludedList<")
                    childDescriptor.toString(this, indent, seen)
                    append('>')
                }

                else -> {
                    append(": ExplicitList<")
                    childDescriptor.toString(this, indent, seen)
                    append('>')
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlListDescriptor

        if (outputKind != other.outputKind) return false
        if (!delimiters.contentEquals(other.delimiters)) return false
//        if (childDescriptor != other.childDescriptor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + outputKind.hashCode()
        result = 31 * result + delimiters.contentHashCode()
        result = 31 * result + childDescriptor.hashCode()
        return result
    }


}
