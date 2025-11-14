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
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.OutputKind
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialException
import nl.adaptivity.xmlutil.serialization.impl.OrderMatrix
import nl.adaptivity.xmlutil.util.CompactFragmentSerializer

public class XmlCompositeDescriptor: XmlValueDescriptor {

    @ExperimentalXmlUtilApi
    internal constructor(
        codecConfig: XML.XmlCodecConfig,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        defaultPreserveSpace: TypePreserveSpace
    ) : super(codecConfig, serializerParent, tagParent) {
        when (val requestedOutputKind =
            codecConfig.config.policy.effectiveOutputKind(serializerParent, tagParent, false)) {
            OutputKind.Element, // fine
            OutputKind.Mixed -> { // only the case for `@XmlValue` elements.
                // Permit this
            }

            else -> codecConfig.config.policy.invalidOutputKind("Composite element: $tagName - Class SerialKinds/composites can only have Element output kinds, not $requestedOutputKind")
        }

        this.defaultPreserveSpace = defaultPreserveSpace
        this._lazyProps = lazy {
            when (val roInfo = initialChildReorderInfo) {
                null -> createElementDescriptors(codecConfig)
                else -> getReorderedElementDescriptors(codecConfig, roInfo)
            }
        }
    }

    private constructor(
        original: XmlCompositeDescriptor,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        overriddenSerializer: KSerializer<*>?,
        typeDescriptor: XmlTypeDescriptor,
        namespaceDecls: List<Namespace>,
        tagNameProvider: XmlDescriptor.() -> Lazy<QName>,
        decoderPropertiesProvider: XmlDescriptor.() -> Lazy<DecoderProperties>,
        isCData: Boolean,
        default: String?,
        defaultPreserveSpace: TypePreserveSpace,
        lazyProps: Lazy<LazyProps>,
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
        this._lazyProps = lazyProps
    }



    @ExperimentalXmlUtilApi
    public override val defaultPreserveSpace: TypePreserveSpace

    override val isIdAttr: Boolean get() = false

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = false

    @ExperimentalXmlUtilApi
    public val valueChild: Int get() = lazyProps.valueChildIdx

    @ExperimentalXmlUtilApi
    public val attrMapChild: Int get() = lazyProps.attrMapChildIdx

    override val outputKind: OutputKind get() = OutputKind.Element
    private val initialChildReorderInfo: Collection<XmlOrderConstraint>?
        get() = typeDescriptor.initialChildReorderInfo

    private val _lazyProps: Lazy<LazyProps>

    private val lazyProps: LazyProps get() = _lazyProps.value

    private val children: List<XmlDescriptor> get() = lazyProps.children

    private fun getReorderedElementDescriptors(
        codecConfig: XML.XmlCodecConfig,
        initialChildReorderInfo: Collection<XmlOrderConstraint>
    ): LazyProps {
        val descriptors = arrayOfNulls<XmlDescriptor>(elementsCount)
        val policy = codecConfig.config.policy

        var attrMapChildIdx =
            if (policy.isStrictOtherAttributes) Int.MAX_VALUE else CompositeDecoder.Companion.UNKNOWN_NAME
        var valueChildIdx = CompositeDecoder.Companion.UNKNOWN_NAME


        fun XmlOrderNode.ensureDescriptor(): XmlDescriptor {
            val elementIdx = this.elementIdx
            return descriptors[elementIdx] ?: Unit.run {
                val parentInfo = ParentInfo(codecConfig.config, this@XmlCompositeDescriptor, elementIdx)

                val canBeAttribute =
                    parentInfo.useAnnIsElement != true && (predecessors.isEmpty()) || predecessors.all {
                        it.ensureDescriptor().outputKind == OutputKind.Attribute
                    }

                from(codecConfig, parentInfo, canBeAttribute = canBeAttribute).also { desc ->
                    descriptors[elementIdx] = desc
                    parentInfo.useAnnIsValue?.let { valueChildIdx = elementIdx }
                    if (parentInfo.useAnnIsOtherAttributes && desc is XmlAttributeMapDescriptor) {
                        attrMapChildIdx = elementIdx
                    } else if (attrMapChildIdx < 0 && policy.isStrictOtherAttributes && desc is XmlAttributeMapDescriptor) {
                        attrMapChildIdx = elementIdx
                    }

                }
            }
        }

        // sequence starts should be independent values that can be ordered in any way
        initialChildReorderInfo.sequenceStarts(elementsCount).let { sequenceStarts ->
            for (orderedSequence in sequenceStarts) {
                for (element in orderedSequence.flatten()) {
                    element.ensureDescriptor()
                }
            }
        }

        val updatedReorderInfo = policy.updateReorderMap(initialChildReorderInfo, descriptors.toList().requireNoNulls())
        if (initialChildReorderInfo != updatedReorderInfo) {
            println("Order constraints updated for validity")
            descriptors.fill(null)
            updatedReorderInfo.sequenceStarts(elementsCount).let { sequenceStarts ->
                for (orderedSequence in sequenceStarts) {
                    for (element in orderedSequence.flatten()) {
                        element.ensureDescriptor()
                    }
                }
            }
        }

        val children = descriptors.requireNoNulls().toList()

        val childReorderInfo: Pair<OrderMatrix, IntArray> =
            initialChildReorderInfo.sequenceStarts(elementsCount).fullFlatten(serialDescriptor, children)

        return LazyProps(
            parent = this,
            children = children,
            attrMapChildIdx = if (attrMapChildIdx == Int.MAX_VALUE) CompositeDecoder.Companion.UNKNOWN_NAME else attrMapChildIdx,
            valueChildIdx = valueChildIdx,
            childReorderMap = childReorderInfo.second,
            childConstraints = childReorderInfo.first,
        )

    }

    private fun createElementDescriptors(
        codecConfig: XML.XmlCodecConfig
    ): LazyProps {
        var valueChildIdx = CompositeDecoder.Companion.UNKNOWN_NAME
        val isStrictOtherAttributes = codecConfig.config.policy.isStrictOtherAttributes

        var attrMapChildIdx = if (isStrictOtherAttributes) Int.MAX_VALUE else -1

        @OptIn(ExperimentalSerializationApi::class)
        val children = List(serialDescriptor.elementsCount) { idx ->
            val parentInfo = ParentInfo(codecConfig.config, this, idx)
            val desc = from(codecConfig, parentInfo, canBeAttribute = true)

            if (parentInfo.useAnnIsValue == true) valueChildIdx = idx
            if (parentInfo.useAnnIsOtherAttributes && desc is XmlAttributeMapDescriptor) {
                attrMapChildIdx = idx
            } else if (attrMapChildIdx < 0 && isStrictOtherAttributes && desc is XmlAttributeMapDescriptor) {
                attrMapChildIdx = idx
            }

            desc
        }

        return LazyProps(parent = this, children, if (attrMapChildIdx == Int.MAX_VALUE) -1 else attrMapChildIdx, valueChildIdx)
    }


    override fun getElementDescriptor(index: Int): XmlDescriptor =
        children[index]

    public val childReorderMap: IntArray? get() = lazyProps.childReorderMap

    public val childConstraints: OrderMatrix? get() = lazyProps.childConstraints

    override fun appendTo(builder: Appendable, indent: Int, seen: MutableSet<String>) {
        builder.apply {
            append(tagName.toString())
                .appendLine(" (")
            var first = true
            if (_lazyProps.isInitialized()) {
                for (child in children) {
                    if (first) first = false else appendLine(',')
                    appendIndent(indent)
                    child.toString(this, indent + 4, seen)
                }
            } else {
                append("<..uninitialized..>")
            }
            appendLine().appendIndent(indent - 4).append(')')
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlCompositeDescriptor

        if (defaultPreserveSpace != other.defaultPreserveSpace) return false
        if (_lazyProps !== other._lazyProps && lazyProps != other.lazyProps) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + defaultPreserveSpace.hashCode()
        result = 31 * result + _lazyProps.hashCode()
        return result
    }

    private class LazyProps(
        parent: XmlCompositeDescriptor,
        val children: List<XmlDescriptor>,
        val attrMapChildIdx: Int,
        val valueChildIdx: Int,
        val childReorderMap: IntArray? = null,
        val childConstraints: OrderMatrix? = null,
    ) {

        init {
            if (valueChildIdx >= 0) {
                val valueChild = children[valueChildIdx]
                @Suppress("OPT_IN_USAGE")
                if (valueChild.serialKind != StructureKind.LIST ||
                    valueChild.getElementDescriptor(0).serialDescriptor.let {
                        @Suppress("DEPRECATION")
                        it != CompactFragmentSerializer.descriptor
                    }
                ) {
                    val invalidIdx = children.indices
                        .firstOrNull { idx -> idx != valueChildIdx && children[idx].outputKind == OutputKind.Element }
                    if (invalidIdx != null) {
                        throw XmlSerialException(
                            "Types (${parent.tagName}) with an @XmlValue member may not contain other child elements (${
                                parent.serialDescriptor.getElementDescriptor(
                                    invalidIdx
                                )
                            }"
                        )
                    }
                }
            }

        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as LazyProps

            if (attrMapChildIdx != other.attrMapChildIdx) return false
            if (valueChildIdx != other.valueChildIdx) return false

            if (children.size != other.children.size) return false
            for (childIdx in children.indices) {
                val c = children[childIdx]
                val oc = other.children[childIdx]
                if (c.tagName != oc.tagName) return false
                if (c.outputKind != oc.outputKind) return false
                if (c.typeDescriptor != oc.typeDescriptor) return false
            }
            if (!childReorderMap.contentEquals(other.childReorderMap)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = attrMapChildIdx
            result = 31 * result + valueChildIdx
            result = 31 * result + children.hashCode()
            result = 31 * result + (childReorderMap?.contentHashCode() ?: 0)
            result = 31 * result + (childConstraints?.hashCode() ?: 0)
            return result
        }


    }
}
