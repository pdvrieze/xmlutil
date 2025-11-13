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
import kotlinx.serialization.descriptors.SerialKind
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.toCName
import nl.adaptivity.xmlutil.toNamespace

internal class ParentInfo(
    config: XmlConfig,
    override val descriptor: XmlDescriptor,
    override val index: Int,
    useNameInfo: XmlSerializationPolicy.DeclaredNameInfo? = null,
    useOutputKind: OutputKind? = null,
    override val overriddenSerializer: KSerializer<*>? = null
) : SafeParentInfo {

    override fun copy(
        config: XmlConfig,
        overriddenSerializer: KSerializer<*>?
    ): ParentInfo {
        return ParentInfo(config, descriptor, index, elementUseNameInfo, elementUseOutputKind, overriddenSerializer)
    }

    override val parentIsInline: Boolean get() = descriptor is XmlInlineDescriptor

    override val namespace: Namespace
        get() = descriptor.tagName.toNamespace()

    @OptIn(ExperimentalSerializationApi::class)
    override val elementTypeDescriptor: XmlTypeDescriptor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        when {
            elementSerialDescriptor.kind == SerialKind.CONTEXTUAL ->
                descriptor.typeDescriptor

            else -> config.lookupTypeDesc(descriptor.tagParent.namespace, elementSerialDescriptor)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override val elementUseAnnotations: Collection<Annotation>
        get() = when (index) {
            -1 -> emptyList()
            else -> descriptor.serialDescriptor.getElementAnnotations(index)
        }

    @ExperimentalXmlUtilApi
    public override var useAnnXmlSerialName: XmlSerialName? = null

    @ExperimentalXmlUtilApi
    public override var useAnnIsElement: Boolean? = null
        private set

    @ExperimentalXmlUtilApi
    public override var useAnnIsValue: Boolean? = null
        private set

    @ExperimentalXmlUtilApi
    public override var useAnnPolyChildren: XmlPolyChildren? = null
        private set

    @ExperimentalXmlUtilApi
    public override var useAnnIgnoreWhitespace: Boolean? = null
        private set

    @ExperimentalXmlUtilApi
    public override var useAnnChildrenName: XmlChildrenName? = null
        private set

    @ExperimentalXmlUtilApi
    public override var useAnnKeyName: XmlKeyName? = null
        private set

    @ExperimentalXmlUtilApi
    public override var useAnnMapEntryName: XmlMapEntryName? = null
        private set

    @ExperimentalXmlUtilApi
    public override var useAnnCData: Boolean? = null
        private set

    @ExperimentalXmlUtilApi
    public override var useAnnIsId: Boolean = false
        private set

    @ExperimentalXmlUtilApi
    public override var useAnnIsOtherAttributes: Boolean = false
        private set

    @ExperimentalXmlUtilApi
    public override var useAnnDefault: String? = null
        private set

    @ExperimentalXmlUtilApi
    public override var useAnnBefore: Array<out String>? = null
        private set

    @ExperimentalXmlUtilApi
    public override var useAnnAfter: Array<out String>? = null
        private set

    @ExperimentalXmlUtilApi
    public override var useAnnNsDecls: List<Namespace>? = null
        private set

    init {
        @OptIn(ExperimentalSerializationApi::class)
        val annotations = descriptor.serialDescriptor.getElementAnnotations(index)
        for (an in annotations) {
            when (an) {
                is XmlSerialName -> useAnnXmlSerialName = an
                is XmlElement -> useAnnIsElement = an.value
                is XmlPolyChildren -> useAnnPolyChildren = an
                is XmlIgnoreWhitespace -> useAnnIgnoreWhitespace = an.value
                is XmlNamespaceDeclSpecs -> useAnnNsDecls = an.namespaces
                is XmlChildrenName -> useAnnChildrenName = an
                is XmlKeyName -> useAnnKeyName = an
                is XmlMapEntryName -> useAnnMapEntryName = an
                is XmlValue -> useAnnIsValue = an.value
                is XmlId -> useAnnIsId = true
                is XmlOtherAttributes -> useAnnIsOtherAttributes = true
                is XmlCData -> useAnnCData = an.value
                is XmlDefault -> useAnnDefault = an.value
                is XmlBefore -> useAnnBefore = an.value
                is XmlAfter -> useAnnAfter = an.value
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override val elementUseNameInfo: XmlSerializationPolicy.DeclaredNameInfo = useNameInfo ?: when (index) {
        -1 -> XmlSerializationPolicy.DeclaredNameInfo(descriptor.serialDescriptor.serialName)
        else -> getElementNameInfo(
            descriptor.serialDescriptor.getElementName(index),
            descriptor.tagName.toNamespace(),
            useAnnXmlSerialName
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    override val elementSerialDescriptor: SerialDescriptor = when {
        overriddenSerializer != null -> overriddenSerializer.descriptor.getXmlOverride()

        descriptor.serialKind == SerialKind.CONTEXTUAL ->
            descriptor.serialDescriptor

        index == -1 -> descriptor.serialDescriptor

        else -> descriptor.serialDescriptor.getElementDescriptor(index).getXmlOverride()
    }

    override val elementUseOutputKind: OutputKind? = useOutputKind ?: when (index) {
        -1 -> null
        else -> when {
            useAnnIsValue == true -> OutputKind.Mixed
            useAnnIsId || useAnnIsOtherAttributes -> OutputKind.Attribute
            useAnnIsElement == true -> OutputKind.Element
            useAnnIsElement == false -> OutputKind.Attribute
            useAnnPolyChildren != null || useAnnChildrenName != null -> OutputKind.Element
            useAnnCData == true -> OutputKind.Element
            else -> null
        }
    }

    override fun toString(): String = buildString {
        append("ParentInfo(")
        append(descriptor.tagName.toCName())
        append('/')
        append(descriptor.serialDescriptor.getElementDescriptor(index).serialName)
        append(")")
    }

    @Suppress("DuplicatedCode")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ParentInfo

        if (index != other.index) return false
//        if (useAnnIsElement != other.useAnnIsElement) return false
//        if (useAnnIsValue != other.useAnnIsValue) return false
//        if (useAnnIgnoreWhitespace != other.useAnnIgnoreWhitespace) return false
//        if (useAnnCData != other.useAnnCData) return false
//        if (useAnnIsId != other.useAnnIsId) return false
//        if (useAnnIsOtherAttributes != other.useAnnIsOtherAttributes) return false
        if (descriptor != other.descriptor) return false
        if (overriddenSerializer != other.overriddenSerializer) return false
//        if (useAnnXmlSerialName != other.useAnnXmlSerialName) return false
//        if (useAnnPolyChildren != other.useAnnPolyChildren) return false
//        if (useAnnChildrenName != other.useAnnChildrenName) return false
//        if (useAnnKeyName != other.useAnnKeyName) return false
//        if (useAnnMapEntryName != other.useAnnMapEntryName) return false
//        if (useAnnDefault != other.useAnnDefault) return false
//        if (!useAnnBefore.contentEquals(other.useAnnBefore)) return false
//        if (!useAnnAfter.contentEquals(other.useAnnAfter)) return false
//        if (useAnnNsDecls != other.useAnnNsDecls) return false
        if (elementUseNameInfo != other.elementUseNameInfo) return false
//        if (elementSerialDescriptor != other.elementSerialDescriptor) return false
//        if (elementUseOutputKind != other.elementUseOutputKind) return false
//        if (elementTypeDescriptor != other.elementTypeDescriptor) return false

        return true
    }

    @Suppress("DuplicatedCode")
    override fun hashCode(): Int {
        var result = index
        result = 31 * result + (useAnnIsElement?.hashCode() ?: 0)
        result = 31 * result + (useAnnIsValue?.hashCode() ?: 0)
        result = 31 * result + (useAnnIgnoreWhitespace?.hashCode() ?: 0)
        result = 31 * result + (useAnnCData?.hashCode() ?: 0)
        result = 31 * result + useAnnIsId.hashCode()
        result = 31 * result + useAnnIsOtherAttributes.hashCode()
        result = 31 * result + descriptor.hashCode()
        result = 31 * result + (overriddenSerializer?.hashCode() ?: 0)
        result = 31 * result + (useAnnXmlSerialName?.hashCode() ?: 0)
        result = 31 * result + (useAnnPolyChildren?.hashCode() ?: 0)
        result = 31 * result + (useAnnChildrenName?.hashCode() ?: 0)
        result = 31 * result + (useAnnKeyName?.hashCode() ?: 0)
        result = 31 * result + (useAnnMapEntryName?.hashCode() ?: 0)
        result = 31 * result + (useAnnDefault?.hashCode() ?: 0)
        result = 31 * result + (useAnnBefore?.contentHashCode() ?: 0)
        result = 31 * result + (useAnnAfter?.contentHashCode() ?: 0)
        result = 31 * result + (useAnnNsDecls?.hashCode() ?: 0)
        result = 31 * result + elementUseNameInfo.hashCode()
        result = 31 * result + elementSerialDescriptor.hashCode()
        result = 31 * result + (elementUseOutputKind?.hashCode() ?: 0)
        result = 31 * result + elementTypeDescriptor.hashCode()
        return result
    }
}
