/*
 * Copyright (c) 2024-2025.
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

@file:Suppress("DEPRECATION")
@file:MustUseReturnValues

package nl.adaptivity.xmlutil.serialization.structure

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.*
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.XML.XmlCodecConfig
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.DeclaredNameInfo
import nl.adaptivity.xmlutil.serialization.impl.QNameMap
import nl.adaptivity.xmlutil.serialization.impl.maybeSerialName

public sealed class XmlDescriptor @XmlUtilInternal constructor(
    override val serializerParent: SafeParentInfo,
    final override val tagParent: SafeParentInfo,
    final override val overriddenSerializer: KSerializer<*>?,
    protected val useNameInfo: DeclaredNameInfo,
    override val typeDescriptor: XmlTypeDescriptor,
    @ExperimentalXmlUtilApi
    public val namespaceDecls: List<Namespace>,
    tagNameProvider: XmlDescriptor.() -> Lazy<QName>,
    decoderPropertiesProvider: XmlDescriptor.() -> Lazy<DecoderProperties>,
) : SafeXmlDescriptor, Iterable<XmlDescriptor> {


    protected constructor(
        codecConfig: XmlCodecConfig,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo = serializerParent
    ) : this(
        serializerParent,
        tagParent,
        overriddenSerializer = serializerParent.overriddenSerializer,
        useNameInfo = serializerParent.elementUseNameInfo,
        typeDescriptor = serializerParent.elementTypeDescriptor,
        namespaceDecls = codecConfig.config.policy.elementNamespaceDecls(serializerParent),
        tagNameProvider = { lazy(LazyThreadSafetyMode.PUBLICATION) { codecConfig.config.policy.effectiveName(serializerParent, tagParent, outputKind, useNameInfo)} },
        decoderPropertiesProvider = { lazy(LazyThreadSafetyMode.PUBLICATION) { DecoderProperties(codecConfig, this) }}
    )

    protected constructor(
        original: XmlDescriptor,
        serializerParent: SafeParentInfo = original.serializerParent,
        tagParent: SafeParentInfo = original.tagParent,
        overriddenSerializer: KSerializer<*>? = original.overriddenSerializer,
        useNameInfo: DeclaredNameInfo = original.useNameInfo,
        typeDescriptor: XmlTypeDescriptor = original.typeDescriptor,
        namespaceDecls: List<Namespace> = original.namespaceDecls,
        tagNameProvider: XmlDescriptor.() -> Lazy<QName> = { original._tagName },
        decoderPropertiesProvider: XmlDescriptor.() -> Lazy<DecoderProperties> = { original._decoderProperties },
    ) : this(
        serializerParent,
        tagParent,
        overriddenSerializer,
        useNameInfo,
        typeDescriptor,
        namespaceDecls,
        tagNameProvider,
        decoderPropertiesProvider,
    )

    /**
     * Does this value represent an xml ID attribute (requiring global uniqueness)
     */
    public abstract val isIdAttr: Boolean

    public open val effectiveOutputKind: OutputKind
        get() {
            return when (val ok = outputKind) {
                OutputKind.Inline -> getElementDescriptor(0).effectiveOutputKind
                else -> ok
            }
        }


    public open val isUnsigned: Boolean get() = false

    internal val _tagName: Lazy<QName> = tagNameProvider()
    override val tagName: QName get() = _tagName.value

    override val serialDescriptor: SerialDescriptor get() = typeDescriptor.serialDescriptor

    internal abstract fun copy(nameProvider: XmlDescriptor.() -> Lazy<QName>): XmlDescriptor

    @OptIn(ExperimentalSerializationApi::class)
    override val elementsCount: Int
        get() = typeDescriptor.serialDescriptor.elementsCount

    @ExperimentalSerializationApi
    override val serialKind: SerialKind
        get() = typeDescriptor.serialDescriptor.kind

    internal val _decoderProperties: Lazy<DecoderProperties> = decoderPropertiesProvider()
    private val decoderProperties: DecoderProperties get() = _decoderProperties.value

    /** Map between tag name and polymorphic info */
    internal val polyMap: QNameMap<PolyInfo> get() = decoderProperties.polyMap

    /** Array of children that are contextual and need delayed resolution */
    internal val contextualChildren: IntArray get() = decoderProperties.contextualChildren

    /** Map between tag names and element index. */
    internal val tagNameMap: QNameMap<Int> get() = decoderProperties.tagNameMap

    /** Map between attribute names and element index. */
    internal val attrMap: QNameMap<Int> get() = decoderProperties.attrMap

    /**
     * This retrieves the descriptor of the actually visible tag (omitting transparent elements).
     * In many cases this is the descriptor itself.
     */
    internal open val visibleDescendantOrSelf: XmlDescriptor get() = this

    @Suppress("UNCHECKED_CAST", "OPT_IN_USAGE")
    internal fun <V> effectiveSerializationStrategy(fallback: SerializationStrategy<V>): SerializationStrategy<V> {
        val oSer = (overriddenSerializer ?: return fallback) as SerializationStrategy<V>
        if (oSer.descriptor.isNullable && oSer == (fallback as? KSerializer<Any>)?.nullable) return fallback
        return oSer
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Suppress("UNCHECKED_CAST")
    internal fun <V> effectiveDeserializationStrategy(fallback: DeserializationStrategy<V>): DeserializationStrategy<V> {
        if (overriddenSerializer == null) return fallback
        if (overriddenSerializer.descriptor.isNullable && !fallback.descriptor.isNullable) {
            if (overriddenSerializer == (fallback as? KSerializer<Any>)?.nullable) return fallback
        }

        return overriddenSerializer as DeserializationStrategy<V>
    }

    public open fun getElementDescriptor(index: Int): XmlDescriptor {
        throw IndexOutOfBoundsException("There are no children")
    }

    @OptIn(ExperimentalSerializationApi::class)
    @IgnorableReturnValue
    internal fun <A : Appendable> toString(builder: A, indent: Int, seen: MutableSet<String>): A {
        when (this) {
            is XmlContextualDescriptor,
            is XmlListDescriptor,
            is XmlPrimitiveDescriptor -> appendTo(builder, indent, seen)

            else -> if (serialDescriptor.serialName in seen) {
                when {
                    _tagName.isInitialized() -> builder.append(tagName.toString())
                    else -> builder.append("<pendingTagName> ")
                }.append("<...> = ").append(outputKind.name)
            } else {
                seen.add(serialDescriptor.serialName)
                appendTo(builder, indent, seen)
            }
        }
        return builder
    }

    override fun iterator(): Iterator<XmlDescriptor> = ElementIterator()

    internal fun overrideDescriptor(
        codecConfig: XmlCodecConfig,
        overriddenDescriptor: SerialDescriptor
    ): XmlDescriptor {
        val ns = tagParent.namespace
        val typeDescriptor = codecConfig.config.lookupTypeDesc(ns, overriddenDescriptor)

        val overriddenParentInfo: SafeParentInfo = DetachedParent(ns, typeDescriptor, useNameInfo)

        return from(codecConfig, overriddenParentInfo, tagParent, outputKind == OutputKind.Attribute)
    }

    internal abstract fun appendTo(builder: Appendable, indent: Int, seen: MutableSet<String>)

    final override fun toString(): String {
        return toString(StringBuilder(), 0, mutableSetOf()).toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XmlDescriptor

        if (overriddenSerializer != other.overriddenSerializer) return false
        if (useNameInfo != other.useNameInfo) return false
        return typeDescriptor == other.typeDescriptor
    }

    override fun hashCode(): Int {
        var result = useNameInfo.hashCode()
        result = 31 * result + typeDescriptor.hashCode()
        result = 31 * result + (overriddenSerializer?.hashCode() ?: 0)
        return result
    }


    private fun DecoderProperties(codecConfig: XmlCodecConfig, xmlDescriptor: XmlDescriptor): DecoderProperties {
        val isUnchecked = codecConfig.config.isUnchecked

        val seenTagNames = HashSet<QName>()
        val localPolyMap = QNameMap<PolyInfo>()
        val localTagNameMap = QNameMap<Int>()
        val localAttrMap = QNameMap<Int>()
        val contextualChildrenList = mutableListOf<Int>()

        val outerValueChild = xmlDescriptor.getValueChild()
        @OptIn(ExperimentalSerializationApi::class)
        for (idx in 0 until xmlDescriptor.elementsCount) {
            val isValueChild = idx == outerValueChild
            val elementDesc = xmlDescriptor.getElementDescriptor(idx).visibleDescendantOrSelf
            when {
                // Transparent polymorphism adds all potential child tags
                elementDesc is XmlPolymorphicDescriptor && elementDesc.isTransparent -> {
                    for (childDescriptor in elementDesc.polyInfo.values) {
                        val tagName = when {
                            isValueChild && childDescriptor.outputKind.isTextOrMixed -> QName("kotlin.String")
                            else -> childDescriptor.tagName.normalize()
                        }
                        check(isUnchecked || seenTagNames.add(tagName)) {
                            "Duplicate name $tagName:$idx as polymorphic child in ${xmlDescriptor.serialDescriptor.serialName}"
                        }
                        localPolyMap[tagName] = PolyInfo(tagName, idx, childDescriptor)
                    }
                }

                elementDesc is XmlMapDescriptor && elementDesc.isListEluded && !elementDesc.isValueCollapsed -> {
                    localTagNameMap[elementDesc.entryName.normalize()] = idx
                }

                elementDesc is XmlContextualDescriptor && xmlDescriptor.serialKind !is PolymorphicKind -> {
                    contextualChildrenList.add(idx)
                }

                elementDesc.effectiveOutputKind == OutputKind.Attribute -> {
                    check(localAttrMap.put(elementDesc.tagName.normalize(), idx) == null || isUnchecked) {
                        "Duplicate name ${elementDesc.tagName} as child in ${xmlDescriptor.serialDescriptor.serialName}"
                    }
                }

                else -> {
                    check(isUnchecked || seenTagNames.add(elementDesc.tagName)) {
                        "Duplicate name ${elementDesc.tagName} as child in ${xmlDescriptor.serialDescriptor.serialName}"
                    }
                    localTagNameMap[elementDesc.tagName.normalize()] = idx

                }


            }
        }

        return DecoderProperties(localPolyMap, localTagNameMap, localAttrMap, contextualChildrenList.toIntArray())
    }

    internal class DecoderProperties(
        val polyMap: QNameMap<PolyInfo>,
        val tagNameMap: QNameMap<Int>,
        val attrMap: QNameMap<Int>,
        val contextualChildren: IntArray,
    )

    private inner class ElementIterator : Iterator<XmlDescriptor> {
        private var pos = 0

        override fun hasNext(): Boolean = pos < elementsCount

        override fun next(): XmlDescriptor = getElementDescriptor(pos++)
    }

    internal companion object {

        /**
         * @param serializerParent The descriptor for the directly preceding serializer. This determines the actual
         *                           serialdescriptor.
         * @param tagParent Parent descriptor from the xml output perspective (ignoring anomymous lists and polymorphic
         *                   subtags). In other words, the parent tag that is the kotlin object that "owns" the value
         *                   (and isn't builtin). It is used to determine the applied annotations and requested tag
         *                   name.
         */
        @OptIn(ExperimentalSerializationApi::class, ExperimentalXmlUtilApi::class)
        internal fun from(
            codecConfig: XmlCodecConfig,
            serializerParent: SafeParentInfo,
            tagParent: SafeParentInfo = serializerParent,
            canBeAttribute: Boolean
        ): XmlDescriptor {

            val overridenSerializer = codecConfig.config.policy.overrideSerializerOrNull(serializerParent, tagParent)

            return codecConfig.config.formatCache.lookupDescriptorOrStore(
                overridenSerializer,
                serializerParent,
                tagParent,
                canBeAttribute
            ) {
                val elementSerialDescriptor: SerialDescriptor
                val effectiveSerializerParent: SafeParentInfo
                val effectiveTagParent: SafeParentInfo

                when (overridenSerializer) {
                    null -> {
                        elementSerialDescriptor = serializerParent.elementSerialDescriptor
                        effectiveSerializerParent = serializerParent
                        effectiveTagParent = tagParent
                    }

                    else -> {
                        elementSerialDescriptor = overridenSerializer.descriptor.getXmlOverride()
                        effectiveSerializerParent = serializerParent.copy(
                            config = codecConfig.config,
                            overriddenSerializer = overridenSerializer
                        )
                        effectiveTagParent = tagParent.copy(
                            config = codecConfig.config,
                            overriddenSerializer = overridenSerializer
                        )
                    }
                }

                val preserveSpace = codecConfig.config.policy.preserveSpace(serializerParent, tagParent)

                when (elementSerialDescriptor.kind) {
                    SerialKind.ENUM,
                    is PrimitiveKind ->
                        XmlPrimitiveDescriptor(
                            codecConfig,
                            effectiveSerializerParent,
                            effectiveTagParent,
                            canBeAttribute,
                            preserveSpace
                        )

                    StructureKind.LIST ->
                        XmlListDescriptor(
                            codecConfig,
                            effectiveSerializerParent,
                            effectiveTagParent,
                            preserveSpace
                        )

                    StructureKind.MAP -> {
                        when {
                            serializerParent.useAnnIsOtherAttributes ->
                                XmlAttributeMapDescriptor(
                                    codecConfig,
                                    effectiveSerializerParent,
                                    effectiveTagParent,
                                    preserveSpace
                                )

                            serializerParent.elementUseOutputKind == OutputKind.Attribute -> XmlAttributeMapDescriptor(
                                codecConfig,
                                effectiveSerializerParent,
                                effectiveTagParent,
                                preserveSpace
                            )

                            else -> XmlMapDescriptor(
                                codecConfig,
                                effectiveSerializerParent,
                                effectiveTagParent,
                                preserveSpace
                            )
                        }
                    }

                    is PolymorphicKind ->
                        XmlPolymorphicDescriptor(
                            codecConfig,
                            effectiveSerializerParent,
                            effectiveTagParent,
                            preserveSpace
                        )

                    SerialKind.CONTEXTUAL ->
                        XmlContextualDescriptor(
                            codecConfig,
                            effectiveSerializerParent,
                            effectiveTagParent,
                            canBeAttribute,
                            preserveSpace
                        )

                    else -> when {
                        codecConfig.config.policy.isInlineCollapsed(serializerParent, tagParent) &&
                                elementSerialDescriptor.isInline ->
                            XmlInlineDescriptor(
                                codecConfig,
                                effectiveSerializerParent,
                                effectiveTagParent,
                                canBeAttribute,
                                preserveSpace
                            )

                        else ->
                            codecConfig.config.formatCache.getCompositeDescriptor(
                                codecConfig,
                                effectiveSerializerParent,
                                effectiveTagParent,
                                preserveSpace
                            )
                    }
                }

            }
        }

    }
}

@Suppress("UNCHECKED_CAST")
internal fun <X: XmlDescriptor> X.copy(name: QName): X = copy( { lazyOf(name) }) as X

/**
 * Determines the usage name information. As such it only looks at the property's serialName and XmlSerialName annotation.
 */
internal fun getElementNameInfo(
    serialName: String,
    parentNamespace: Namespace?,
    annotation: XmlSerialName?
): DeclaredNameInfo {
    val qName = annotation?.toQName(serialName, parentNamespace)
    return DeclaredNameInfo(
        serialName,
        qName,
        qName?.prefix != "xml" && annotation?.namespace == UNSET_ANNOTATION_VALUE
    )
}

/*
 * Determines the type name information. This means it will
 */
@ExperimentalSerializationApi
internal fun SerialDescriptor.getNameInfo(
    config: XmlConfig,
    parentNamespace: Namespace?,
    annotation: XmlSerialName?
): DeclaredNameInfo {
    val realSerialName = when {
        isNullable && serialName.endsWith('?') -> serialName.dropLast(1)
        else -> capturedKClass?.maybeSerialName ?: serialName
    }

    val policySerialName =
        config.policy.serialTypeNameToQName(DeclaredNameInfo(realSerialName), parentNamespace?: DEFAULT_NAMESPACE).localPart

    val qName = annotation?.toQName(policySerialName, parentNamespace) ?: (this as? XmlSerialDescriptor)?.serialQName
    return DeclaredNameInfo(realSerialName, qName, annotation?.namespace == UNSET_ANNOTATION_VALUE)
}

internal val DEFAULT_NAMESPACE = XmlEvent.NamespaceImpl("", "")

@ExperimentalXmlUtilApi
internal fun XmlTypeDescriptor.declOutputKind(): OutputKind? = when {
    typeAnnIsXmlValue == true -> OutputKind.Text
    typeAnnIsId -> OutputKind.Attribute
    typeAnnIsElement == true -> OutputKind.Element
    typeAnnIsElement == false -> OutputKind.Attribute
    typeAnnChildrenName != null || typeAnnPolyChildren != null -> OutputKind.Element
    else -> null
}


@IgnorableReturnValue
internal fun <A : Appendable> A.appendIndent(count: Int) = apply {
    for (i in 0 until count) {
        append(' ')
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.getXmlOverride() = when {
    this is XmlSerialDescriptor -> xmlDescriptor
    isNullable &&
            annotations.hasXmlSerialDescriptorMarker -> getElementDescriptor(-1).nullable

    else -> this
}

internal fun QName.normalize(): QName {
    return copy(prefix = "")
}

private val List<Annotation>.hasXmlSerialDescriptorMarker: Boolean
    get() {
        if (isEmpty()) return false
        return get(0) is XmlSerialDescriptorMarker
    }
