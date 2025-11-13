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
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.toNamespace

public class XmlListDescriptor internal constructor(
    codecConfig: XML.XmlCodecConfig,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
    preserveSpace: TypePreserveSpace
) : XmlListLikeDescriptor(codecConfig, serializerParent, tagParent, preserveSpace) {

    override val outputKind: OutputKind

    override val isIdAttr: Boolean get() = false

    public val delimiters: Array<String>

    init {
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
                            DetachedParent(namespace, childTypeDescriptor,
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
    }

    private val childDescriptor: XmlDescriptor by lazy(LazyThreadSafetyMode.PUBLICATION) {
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
