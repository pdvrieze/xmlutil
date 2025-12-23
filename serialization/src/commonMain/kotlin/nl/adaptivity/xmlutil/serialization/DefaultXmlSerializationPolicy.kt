/*
 * Copyright (c) 2020-2025.
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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.core.impl.multiplatform.computeIfAbsent
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.XmlEncodeDefault
import nl.adaptivity.xmlutil.serialization.structure.*

/**
 * Default implementation of a serialization policy that provides a behaviour that attempts to create an XML format
 * that resembles what would be created manually in line with XML's design.
 *
 * @property pedantic Enable some stricter behaviour
 * @property autoPolymorphic Should polymorphic information be retrieved using [kotlinx.serialization.modules.SerializersModule] configuration. This replaces
 *  *                     [XmlPolyChildren], but changes serialization where that annotation is not applied. This option will
 *  *                     become the default in the future although XmlPolyChildren will retain precedence (when present)
 * @property encodeDefault Determine whether defaults need to be encoded
 * @property unknownChildHandler A function that is called when an unknown child is found. By default an exception is thrown
 *  *                     but the function can silently ignore it as well.
 * @property typeDiscriminatorName When set, use a type discriminator property
 * @property throwOnRepeatedElement When a single-value elemement is repeated in the content, will this throw an
 *   exception or only retain the final value
 * @property verifyElementOrder Verify that element children are in the order required by order annotations (and
 *   fail if not correct). Note that attribute order in XML is arbitrary and not meaningful.
 * @property isStrictAttributeNames Process attribute name reading strictly according to the XML standard, or a
 *   name handling that is a bit more lenient
 * @property isStrictBoolean Parse boolean data according to the requirements of XML, rather than the (very lenient)
 *   toBoolean function from the Kotlin standard library.
 * @property isXmlFloat Serialize float/double data according to the requirements of XML, rather than the
 *   toFloat/Float.toString/toDouble/Double.toString function from the Kotlin standard library.
 */
@OptIn(ExperimentalSubclassOptIn::class)
@SubclassOptInRequired(ExperimentalXmlUtilApi::class)
public open class DefaultXmlSerializationPolicy(builder: Builder) : XmlSerializationPolicy {
    @ExperimentalXmlUtilApi
    public val formatCache: FormatCache = builder.formatCache
    public val pedantic: Boolean = builder.pedantic
    public val autoPolymorphic: Boolean = builder.autoPolymorphic
    public val encodeDefault: XmlEncodeDefault = builder.encodeDefault
    public val unknownChildHandler: UnknownChildHandler = builder.unknownChildHandler
    public val typeDiscriminatorName: QName? = builder.typeDiscriminatorName
    public val throwOnRepeatedElement: Boolean = builder.throwOnRepeatedElement
    public final override val verifyElementOrder: Boolean = builder.verifyElementOrder
    public final override val isStrictAttributeNames: Boolean = builder.isStrictAttributeNames
    public final override val isStrictBoolean: Boolean = builder.isStrictBoolean
    public final override val isXmlFloat: Boolean = builder.isXmlFloat
    public final override val isStrictOtherAttributes: Boolean = builder.isStrictOtherAttributes

    /**
     * Determines whether inline classes are merged with their content. Note that inline classes
     * may still determine the tag name used for the data even if the actual contents come from
     * the child content.
     *
     * This value is used by default by the implementation of [isInlineCollapsed].
     *
     * If the value is `false` inline classes will be handled like non-inline classes
     */
    public val isInlineCollapsedDefault: Boolean = builder.isInlineCollapsedDefault

    @ExperimentalXmlUtilApi
    public override val defaultPrimitiveOutputKind: OutputKind = builder.defaultPrimitiveOutputKind

    @ExperimentalXmlUtilApi
    public override val defaultObjectOutputKind: OutputKind = builder.defaultObjectOutputKind

    // region Secondary constructors

    @OptIn(ExperimentalXmlUtilApi::class)
    public constructor(original: XmlSerializationPolicy?) : this(
        (original as? DefaultXmlSerializationPolicy)?.builder()
            ?: original?.let { Builder10(it) }
            ?: Builder10()
    )

    public constructor(config: Builder10.() -> Unit) : this(Builder10().apply(config))

    //endregion

    override fun polymorphicDiscriminatorName(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): QName? {
        return typeDiscriminatorName
    }

    override fun isListEluded(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
    ): Boolean {
        if (tagParent.useAnnIsValue == true) return true

        return tagParent.useAnnChildrenName == null
    }

    override fun isTransparentPolymorphic(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
    ): Boolean {
        return autoPolymorphic || tagParent.useAnnPolyChildren != null
    }

    @OptIn(ExperimentalSerializationApi::class, ExperimentalXmlUtilApi::class)
    override fun effectiveOutputKind(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        canBeAttribute: Boolean
    ): OutputKind {
        val serialDescriptor = overrideSerializerOrNull(serializerParent, tagParent)?.descriptor
            ?.getXmlOverride()
            ?: serializerParent.elementSerialDescriptor

        return when (val overrideOutputKind = serializerParent.elementUseOutputKind) {
            null -> {
                val isValue = tagParent.useAnnIsValue == true
                var parentChildDesc = tagParent.elementSerialDescriptor
                while (parentChildDesc.isInline) {
                    parentChildDesc =
                        parentChildDesc.getElementDescriptor(0)
                }
                val elementKind = parentChildDesc.kind
                // If we can't be an attribue
                when {
                    elementKind == StructureKind.CLASS -> OutputKind.Element

                    isValue -> OutputKind.Mixed

                    !canBeAttribute && (tagParent.elementUseOutputKind == OutputKind.Attribute) ->
                        handleAttributeOrderConflict(serializerParent, tagParent, OutputKind.Attribute)

                    !canBeAttribute -> OutputKind.Element

                    else -> tagParent.elementUseOutputKind
                        ?: serializerParent.elementTypeDescriptor.declOutputKind()
                        ?: defaultOutputKind(serialDescriptor.kind)
                }
            }

            OutputKind.Mixed -> {
                if (serializerParent.descriptor is XmlListDescriptor) {
                    if (tagParent.elementSerialDescriptor.kind == StructureKind.CLASS) {
                        OutputKind.Element
                    } else {
                        OutputKind.Mixed
                    }
                } else {
                    val outputKind = tagParent.elementUseOutputKind
                        ?: serializerParent.elementTypeDescriptor.declOutputKind()
                        ?: defaultOutputKind(serialDescriptor.kind)

                    when (outputKind) {
                        OutputKind.Attribute -> OutputKind.Text
                        else -> outputKind
                    }
                }
            }

            else -> overrideOutputKind

        }
    }

    override fun serialTypeNameToQName(
        typeNameInfo: XmlSerializationPolicy.DeclaredNameInfo,
        parentNamespace: Namespace
    ): QName {
        typeNameInfo.annotatedName?.let { return it }
        return when (val serialName = typeNameInfo.serialName) {
            "kotlin.Boolean" -> QName(XMLConstants.XSD_NS_URI, "boolean", XMLConstants.XSD_PREFIX)
            "kotlin.Byte" -> QName(XMLConstants.XSD_NS_URI, "byte", XMLConstants.XSD_PREFIX)
            "kotlin.UByte" -> QName(XMLConstants.XSD_NS_URI, "unsignedByte", XMLConstants.XSD_PREFIX)
            "kotlin.Short" -> QName(XMLConstants.XSD_NS_URI, "short", XMLConstants.XSD_PREFIX)
            "kotlin.UShort" -> QName(XMLConstants.XSD_NS_URI, "unsignedShort", XMLConstants.XSD_PREFIX)
            "kotlin.Int" -> QName(XMLConstants.XSD_NS_URI, "int", XMLConstants.XSD_PREFIX)
            "kotlin.UInt" -> QName(XMLConstants.XSD_NS_URI, "unsignedInt", XMLConstants.XSD_PREFIX)
            "kotlin.Long" -> QName(XMLConstants.XSD_NS_URI, "long", XMLConstants.XSD_PREFIX)
            "kotlin.ULong" -> QName(XMLConstants.XSD_NS_URI, "unsignedLong", XMLConstants.XSD_PREFIX)
            "kotlin.Float",
            "kotlin.Double" -> QName(XMLConstants.XSD_NS_URI, "double", XMLConstants.XSD_PREFIX)

            "kotlin.String" -> QName(XMLConstants.XSD_NS_URI, "string", XMLConstants.XSD_PREFIX)

            else -> commonSerialNameToQName(serialName, parentNamespace)
        }
    }

    override fun serialUseNameToQName(
        useNameInfo: XmlSerializationPolicy.DeclaredNameInfo,
        parentNamespace: Namespace
    ): QName {
        useNameInfo.annotatedName?.let { return it }
        return commonSerialNameToQName(useNameInfo.serialName, parentNamespace)

    }

    private fun commonSerialNameToQName(
        serialName: String,
        parentNamespace: Namespace
    ): QName {
        var start = 0
        var end = serialName.length
        val namespaceUri = if (serialName[0] == '{') {
            val e = serialName.indexOf('}', 1)
            require(e >= 0) { "Serialname starts with '{' to indicate namespace but does not have a closing '}'" }
            start = e + 1 // skip the namespace in the next step
            serialName.substring(0, e)
        } else {
            parentNamespace.namespaceURI
        }


        for (i in start until serialName.length) {
            when (val c = serialName[i]) {
                '{', '}', ']', ')', '>', ':' -> throw IllegalArgumentException("Unexpected '$c' when determining local name from serialname (\"$serialName\")")
                '(', '<', '[' -> { // allow these to terminate the name
                    end = i
                    break
                }

                '.' -> start = i + 1
            }
        }
        return QName(namespaceUri, serialName.substring(start, end))
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun effectiveName(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        outputKind: OutputKind,
        useName: XmlSerializationPolicy.DeclaredNameInfo
    ): QName {
        val typeDescriptor = serializerParent.elementTypeDescriptor
        val serialKind = typeDescriptor.serialDescriptor.kind
        val typeNameInfo = typeDescriptor.typeNameInfo
        val parentNamespace: Namespace = tagParent.namespace

        assert(typeNameInfo == typeDescriptor.typeNameInfo) {
            "Type name info should match"
        }

        val parentSerialKind = tagParent.descriptor?.serialKind

        val name = when {
            outputKind == OutputKind.Attribute -> when {
                useName.isDefaultNamespace -> QName(useName.annotatedName?.getLocalPart() ?: useName.serialName)
                useName.annotatedName != null -> useName.annotatedName
                else -> QName(useName.serialName)
            } // Use non-prefix attributes by default

            useName.annotatedName != null -> useName.annotatedName

            useName.serialName != typeNameInfo.serialName &&
                    (serialKind is PrimitiveKind ||
                            serialKind == StructureKind.MAP ||
                            serialKind == StructureKind.LIST ||
                            serialKind == PolymorphicKind.OPEN ||
                            typeNameInfo.serialName == "kotlin.Unit" || // Unit needs a special case
                            parentSerialKind is PolymorphicKind) // child of explict polymorphic uses predefined names
                -> serialUseNameToQName(useName, parentNamespace)

            typeNameInfo.annotatedName != null -> typeNameInfo.annotatedName

            else -> serialTypeNameToQName(typeNameInfo, parentNamespace)
        }

        if (pedantic) {
            require(name.localPart !in ILLEGALNAMES) { "QNames may not have reserved names as value :${name.localPart}" }
            require(name.prefix != XMLConstants.XMLNS_ATTRIBUTE) { "XML Namespaces should not be specified as attributes, but rather using the `XmlNamespaceDeclSpecs` annotation" }
            require(name.prefix != XMLConstants.XML_NS_PREFIX || name.namespaceURI == XMLConstants.XML_NS_URI) {
                "The `xml` prefixes may only map to the standard xml namespace (${XMLConstants.XML_NS_URI})"
            }
        }

        return name
    }

    override fun shouldEncodeElementDefault(elementDescriptor: XmlDescriptor?): Boolean {
        return when (encodeDefault) {
            XmlEncodeDefault.NEVER -> false
            XmlEncodeDefault.ALWAYS -> true
            XmlEncodeDefault.ANNOTATED -> (elementDescriptor as? XmlValueDescriptor)?.default == null
        }
    }

    @ExperimentalXmlUtilApi
    @Suppress("DirectUseOfResultType")
    override fun handleUnknownContentRecovering(
        input: XmlReader,
        inputKind: InputKind,
        descriptor: XmlDescriptor,
        name: QName?,
        candidates: Collection<Any>
    ): List<XML.ParsedData<*>> {
        return unknownChildHandler.handleUnknownChildRecovering(input, inputKind, descriptor, name, candidates)
    }

    override fun onElementRepeated(parentDescriptor: XmlDescriptor, childIndex: Int) {
        if (throwOnRepeatedElement) {
            throw XmlSerialException("Duplicate child (${parentDescriptor.friendlyChildName(childIndex)} found in ${parentDescriptor.tagName} outside of eluded list context")
        }
    }

    override fun overrideSerializerOrNull(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
    ): KSerializer<*>? {
        return null
    }

    override fun isInlineCollapsed(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
    ): Boolean {
        return isInlineCollapsedDefault
    }

    /**
     * Default implementation that uses [XmlBefore] and [XmlAfter]. It does
     * not use the parent descriptor at all.
     */
    @OptIn(ExperimentalSerializationApi::class)
    override fun initialChildReorderMap(
        parentDescriptor: SerialDescriptor
    ): Collection<XmlOrderConstraint>? {
        val mapCapacity = parentDescriptor.elementsCount * 2
        val nameToIdx = HashMap<String, Int>(mapCapacity)
        for (i in 0 until parentDescriptor.elementsCount) {
            nameToIdx[parentDescriptor.getElementName(i)] = i
        }

        fun String.toChildIndex(): Int = when (this) {
            "*" -> XmlOrderConstraint.OTHERS
            else -> nameToIdx[this]
                ?: throw XmlSerialException("Could not find the attribute in ${parentDescriptor.serialName} with the name: $this\n  Candidates were: ${nameToIdx.keys.joinToString()}")
        }

        val orderConstraints = HashSet<XmlOrderConstraint>(mapCapacity)
        val orderNodes = HashMap<String, XmlOrderNode>(mapCapacity)

        for (elementIdx in 0 until parentDescriptor.elementsCount) {
            var xmlBefore: Array<out String>? = null
            var xmlAfter: Array<out String>? = null
            for (annotation in parentDescriptor.getElementAnnotations(elementIdx)) {
                if (annotation is XmlBefore && annotation.value.isNotEmpty()) {
                    annotation.value.mapTo(orderConstraints) {
                        val successorIdx = it.toChildIndex()
                        XmlOrderConstraint(elementIdx, successorIdx)
                    }
                    xmlBefore = annotation.value
                } else if (annotation is XmlAfter && annotation.value.isNotEmpty()) {
                    annotation.value.mapTo(orderConstraints) {
                        val predecessorIdx = it.toChildIndex()
                        XmlOrderConstraint(predecessorIdx, elementIdx)
                    }
                    xmlAfter = annotation.value
                }
                if (xmlBefore != null || xmlAfter != null) {
                    val node = orderNodes.computeIfAbsent(
                        parentDescriptor.getElementName(elementIdx)
                    ) {
                        XmlOrderNode(
                            elementIdx
                        )
                    }
                    if (xmlBefore != null) {
                        val befores = Array(xmlBefore.size) {
                            val name = xmlBefore[it]
                            orderNodes.computeIfAbsent(name) { XmlOrderNode(name.toChildIndex()) }
                        }
                        node.addSuccessors(*befores)
                    }
                    if (xmlAfter != null) {
                        val afters = Array(xmlAfter.size) {
                            val name = xmlAfter[it]
                            orderNodes.computeIfAbsent(name) { XmlOrderNode(name.toChildIndex()) }
                        }
                        node.addPredecessors(*afters)
                    }

                }
            }
        }
        if (orderNodes.isEmpty()) return null // no order nodes, no reordering

        return if (orderConstraints.isEmpty()) null else orderConstraints.toList()
    }

    @OptIn(ExperimentalSerializationApi::class)
    @ExperimentalXmlUtilApi
    override fun preserveSpace(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): TypePreserveSpace {
        val b = serializerParent.descriptor?.defaultPreserveSpace ?: TypePreserveSpace.DEFAULT

        return b.overrideIgnore(serializerParent.useAnnIgnoreWhitespace)
    }

    override fun mapKeyName(serializerParent: SafeParentInfo): XmlSerializationPolicy.DeclaredNameInfo {
        val an = serializerParent.useAnnKeyName
        if (an != null) {
            return XmlSerializationPolicy.DeclaredNameInfo(
                an.value,
                an.toQName(serializerParent.namespace),
                an.namespace == UNSET_ANNOTATION_VALUE
            )
        }

        return XmlSerializationPolicy.DeclaredNameInfo("key")
    }

    override fun mapValueName(
        serializerParent: SafeParentInfo,
        isListEluded: Boolean
    ): XmlSerializationPolicy.DeclaredNameInfo {
        val childAnnotation = serializerParent.useAnnChildrenName
        val childrenName = childAnnotation?.toQName(serializerParent.namespace)
        return XmlSerializationPolicy.DeclaredNameInfo(
            "value",
            childrenName,
            childAnnotation?.namespace == UNSET_ANNOTATION_VALUE
        )
    }

    override fun mapEntryName(serializerParent: SafeParentInfo, isListEluded: Boolean): QName {
        val useAnnEntryName = serializerParent.useAnnMapEntryName
        if (useAnnEntryName != null) {
            return useAnnEntryName.toQName(serializerParent.namespace)
        }

        if (isListEluded) { // If we don't have list tags, use the list name, otherwise use the default
            serializerParent.elementUseNameInfo.annotatedName?.let { return it }
        }
        return QName(serializerParent.namespace.namespaceURI, "entry")
    }

    @Suppress("DEPRECATION")
    private val pseudoConfig = XmlConfig(XmlConfig.CompatBuilder(policy = this))

    @OptIn(ExperimentalSerializationApi::class)
    override fun isMapValueCollapsed(mapParent: SafeParentInfo, valueDescriptor: XmlDescriptor): Boolean {
        if (mapParent.useAnnMapEntryName != null) return false
        val keyDescriptor = mapParent.elementSerialDescriptor.getElementDescriptor(0)
        val keyUseName = mapKeyName(mapParent)

        val pseudoKeyParent =
            InjectedParentTag(
                0,
                XmlTypeDescriptor(pseudoConfig, keyDescriptor, mapParent.namespace),
                keyUseName,
                mapParent.namespace
            )
        val keyEffectiveOutputKind = effectiveOutputKind(pseudoKeyParent, pseudoKeyParent, true)
        if (!keyEffectiveOutputKind.isTextual) return false

        val keyName = effectiveName(pseudoKeyParent, pseudoKeyParent, keyEffectiveOutputKind, keyUseName)

        (0 until valueDescriptor.elementsCount)
            .map { valueDescriptor.getElementDescriptor(it) }
            .forEach { elem ->
                if (elem.tagName.isEquivalent(keyName)) return false
            }
        return true
    }

    @OptIn(ExperimentalSerializationApi::class)
    @ExperimentalXmlUtilApi
    override fun elementNamespaceDecls(serializerParent: SafeParentInfo): List<Namespace> {
        return buildList {
            serializerParent.useAnnNsDecls?.let { addAll(it) }
            serializerParent.elementTypeDescriptor.typeAnnNsDecls?.let { addAll(it) }
        }
    }

    override fun ignoredSerialInfo(message: String) {
        if (pedantic) throw XmlSerialException(message)
    }

    /**
     * Create a builder for this policy. This function allows subclasses to have their own configuration.
     */
    public open fun builder(): Builder = Builder10(this)

    /**
     * Create a copy of this configuration with the changes specified through the config parameter.
     */
    @ExperimentalXmlUtilApi
    public inline fun copy(config: Builder.() -> Unit): DefaultXmlSerializationPolicy {
        return builder().apply(config).build()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultXmlSerializationPolicy

        if (formatCache != other.formatCache) return false
        if (pedantic != other.pedantic) return false
        if (autoPolymorphic != other.autoPolymorphic) return false
        if (encodeDefault != other.encodeDefault) return false
        if (unknownChildHandler != other.unknownChildHandler) return false
        if (typeDiscriminatorName != other.typeDiscriminatorName) return false
        if (throwOnRepeatedElement != other.throwOnRepeatedElement) return false
        if (verifyElementOrder != other.verifyElementOrder) return false
        if (isStrictAttributeNames != other.isStrictAttributeNames) return false
        if (isStrictBoolean != other.isStrictBoolean) return false
        if (this@DefaultXmlSerializationPolicy.isXmlFloat != other.isXmlFloat) return false
        if (isStrictOtherAttributes != other.isStrictOtherAttributes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = formatCache.hashCode()
        result = 31 * result + pedantic.hashCode()
        result = 31 * result + autoPolymorphic.hashCode()
        result = 31 * result + encodeDefault.hashCode()
        result = 31 * result + unknownChildHandler.hashCode()
        result = 31 * result + (typeDiscriminatorName?.hashCode() ?: 0)
        result = 31 * result + throwOnRepeatedElement.hashCode()
        result = 31 * result + verifyElementOrder.hashCode()
        result = 31 * result + isStrictAttributeNames.hashCode()
        result = 31 * result + isStrictBoolean.hashCode()
        result = 31 * result + this@DefaultXmlSerializationPolicy.isXmlFloat.hashCode()
        result = 31 * result + isStrictOtherAttributes.hashCode()
        return result
    }


    /**
     * A configuration builder for the default serialization policy.
     *
     * @property formatCache The cache used to speed up mapping for serializer to xml representation.
     * @property pedantic Enable some stricter behaviour
     * @property autoPolymorphic Rather than using type wrappers use the tag name to distinguish polymorphic types
     * @property encodeDefault Determine whether defaults need to be encoded
     * @property unknownChildHandler Function called when an unknown child is encountered. By default it throws an
     *   exception, but this function can use its own recovery behaviour
     * @property typeDiscriminatorName When set, use a type discriminator property
     * @property throwOnRepeatedElement When a single-value elemement is repeated in the content, will this throw an
     *   exception or only retain the final value
     * @property verifyElementOrder Verify that element children are in the order required by order annotations (and
     *   fail if not correct). Note that attribute order in XML is arbitrary and not meaningful.
     * @property isStrictAttributeNames Process attribute name reading strictly according to the XML standard, or a
     *   name handling that is a bit more lenient
     * @property isStrictBoolean Parse boolean data according to the requirements of XML, rather than the (very lenient)
     *   toBoolean function from the Kotlin standard library.
     * @property isXmlFloat Parse float/double data according to the requirements of XML, rather than the
     *   toFloat/Float.toString functions from the Kotlin standard library.
     */
    @OptIn(ExperimentalXmlUtilApi::class)
    @XmlConfigDsl
    public abstract class Builder protected constructor(
        public var pedantic: Boolean,
        public var autoPolymorphic: Boolean,
        public var encodeDefault: XmlEncodeDefault,
        public var unknownChildHandler: UnknownChildHandler,
        public var typeDiscriminatorName: QName?,
        public var throwOnRepeatedElement: Boolean,
        public var verifyElementOrder: Boolean,
        public var isInlineCollapsedDefault: Boolean,
        public var isStrictAttributeNames: Boolean,
        public var isStrictBoolean: Boolean,
        public var isXmlFloat: Boolean,
        public var isStrictOtherAttributes: Boolean,
        @ExperimentalXmlUtilApi
        public var formatCache: FormatCache,
        public var defaultPrimitiveOutputKind: OutputKind,
        public var defaultObjectOutputKind: OutputKind,
    ) {

        /**
         * Constructor for default builder. To set any values, use the property setters. The primary constructor
         * is internal as it is not stable as new configuration options are added.
         */
        private constructor() : this(
            pedantic = false,
            autoPolymorphic = false,
            encodeDefault = XmlEncodeDefault.ANNOTATED,
            unknownChildHandler = XmlConfig.DEFAULT_UNKNOWN_CHILD_HANDLER,
            typeDiscriminatorName = null,
            throwOnRepeatedElement = false,
            verifyElementOrder = false,
            isInlineCollapsedDefault = true,
            isStrictAttributeNames = false,
            isStrictBoolean = false,
            isXmlFloat = false,
            isStrictOtherAttributes = false,
            formatCache = try {
                defaultSharedFormatCache()
            } catch (e: Error) {
                FormatCache.Dummy
            },
            defaultPrimitiveOutputKind = OutputKind.Attribute,
            defaultObjectOutputKind = OutputKind.Element,
        )

        @ExperimentalXmlUtilApi
        protected constructor(policy: DefaultXmlSerializationPolicy) : this(
            pedantic = policy.pedantic,
            autoPolymorphic = policy.autoPolymorphic,
            encodeDefault = policy.encodeDefault,
            unknownChildHandler = policy.unknownChildHandler,
            typeDiscriminatorName = policy.typeDiscriminatorName,
            throwOnRepeatedElement = policy.throwOnRepeatedElement,
            verifyElementOrder = policy.verifyElementOrder,
            isInlineCollapsedDefault = policy.isInlineCollapsedDefault,
            isStrictAttributeNames = policy.isStrictAttributeNames,
            isStrictBoolean = policy.isStrictBoolean,
            isXmlFloat = policy.isXmlFloat,
            isStrictOtherAttributes = policy.isStrictOtherAttributes,
            formatCache = policy.formatCache.copy(),
            defaultPrimitiveOutputKind = policy.defaultPrimitiveOutputKind,
            defaultObjectOutputKind = policy.defaultObjectOutputKind,
        )

        @ExperimentalXmlUtilApi
        protected constructor(policy: XmlSerializationPolicy) : this(
            pedantic = (policy as? DefaultXmlSerializationPolicy)?.pedantic ?: false,
            autoPolymorphic = (policy as? DefaultXmlSerializationPolicy)?.autoPolymorphic ?: false,
            encodeDefault = (policy as? DefaultXmlSerializationPolicy)?.encodeDefault
                ?: XmlEncodeDefault.ANNOTATED,
            unknownChildHandler = (policy as? DefaultXmlSerializationPolicy)?.unknownChildHandler
                ?: XmlConfig.DEFAULT_UNKNOWN_CHILD_HANDLER,
            typeDiscriminatorName = (policy as? DefaultXmlSerializationPolicy)?.typeDiscriminatorName,
            throwOnRepeatedElement = (policy as? DefaultXmlSerializationPolicy)?.throwOnRepeatedElement ?: false,
            verifyElementOrder = policy.verifyElementOrder,
            isInlineCollapsedDefault = (policy as? DefaultXmlSerializationPolicy)?.isInlineCollapsedDefault ?: true,
            isStrictAttributeNames = policy.isStrictAttributeNames,
            isStrictBoolean = policy.isStrictBoolean,
            isXmlFloat = policy.isXmlFloat,
            isStrictOtherAttributes = policy.isStrictOtherAttributes,
            formatCache = (policy as? DefaultXmlSerializationPolicy)?.formatCache?.copy() ?: try {
                defaultSharedFormatCache()
            } catch (e: Error) {
                FormatCache.Dummy
            },
            defaultPrimitiveOutputKind = policy.defaultPrimitiveOutputKind,
            defaultObjectOutputKind = policy.defaultObjectOutputKind,
        )


        public open fun build(): DefaultXmlSerializationPolicy =
            DefaultXmlSerializationPolicy(this)

        public fun ignoreUnknownChildren() {
            unknownChildHandler = XmlConfig.IGNORING_UNKNOWN_CHILD_HANDLER
        }

        public fun ignoreNamespaces() {
            unknownChildHandler = XmlConfig.IGNORING_UNKNOWN_NAMESPACE_HANDLER
        }

    }

    public class BuilderCompat: Builder {
        /**
         * Constructor for default builder. To set any values, use the property setters. The primary constructor
         * is internal as it is not stable as new configuration options are added.
         */
        public constructor() : super(
            pedantic = false,
            autoPolymorphic = false,
            encodeDefault = XmlEncodeDefault.ANNOTATED,
            unknownChildHandler = XmlConfig.DEFAULT_UNKNOWN_CHILD_HANDLER,
            typeDiscriminatorName = null,
            throwOnRepeatedElement = false,
            verifyElementOrder = false,
            isInlineCollapsedDefault = true,
            isStrictAttributeNames = false,
            isStrictBoolean = false,
            isXmlFloat = false,
            isStrictOtherAttributes = false,
            formatCache = try {
                defaultSharedFormatCache()
            } catch (e: Error) {
                FormatCache.Dummy
            },
            defaultPrimitiveOutputKind = OutputKind.Attribute,
            defaultObjectOutputKind = OutputKind.Element,
        )

        public constructor(policy: DefaultXmlSerializationPolicy) : super(policy)
        public constructor(policy: XmlSerializationPolicy) : super(policy)

        public fun setDefaults_0_86_3() {
            autoPolymorphic = true
            pedantic = false
            typeDiscriminatorName = QName(XMLConstants.XSI_NS_URI, "type", XMLConstants.XSI_PREFIX)
            encodeDefault = XmlEncodeDefault.ANNOTATED
            throwOnRepeatedElement = true
            isStrictAttributeNames = true
            isInlineCollapsedDefault = true
        }

        public fun setDefaults_0_87_0() {
            setDefaults_0_86_3()
            isStrictOtherAttributes = true
        }

        public fun setDefaults_0_90_2() {
            setDefaults_0_87_0()
            isStrictBoolean = true
        }

        @Suppress("FunctionName")
        @ExperimentalXmlUtilApi
        public fun setDefaults_0_91_0() {
            setDefaults_0_90_2()
            isXmlFloat = true
        }
    }

    public class Builder10 : Builder {
        /**
         * Constructor for default builder. To set any values, use the property setters. The primary constructor
         * is internal as it is not stable as new configuration options are added.
         */
        public constructor() : super(
            pedantic = false,
            autoPolymorphic = true,
            encodeDefault = XmlEncodeDefault.ANNOTATED,
            unknownChildHandler = XmlConfig.DEFAULT_UNKNOWN_CHILD_HANDLER,
            typeDiscriminatorName = null,
            throwOnRepeatedElement = true,
            verifyElementOrder = false,
            isInlineCollapsedDefault = true,
            isStrictAttributeNames = true,
            isStrictBoolean = true,
            isXmlFloat = true,
            isStrictOtherAttributes = true,
            formatCache = try { defaultSharedFormatCache() } catch (_: Error) { FormatCache.Dummy },
            defaultPrimitiveOutputKind = OutputKind.Attribute,
            defaultObjectOutputKind = OutputKind.Element,
        ) {
            typeDiscriminatorName = QName(XMLConstants.XSI_NS_URI, "type", XMLConstants.XSI_PREFIX)
        }

        public constructor(policy: DefaultXmlSerializationPolicy) : super(policy)
        public constructor(policy: XmlSerializationPolicy) : super(policy)

        @Suppress("FunctionName")
        @ExperimentalXmlUtilApi
        public fun setDefaults_1_0_0() {
            autoPolymorphic = true
            pedantic = false
            typeDiscriminatorName = QName(XMLConstants.XSI_NS_URI, "type", XMLConstants.XSI_PREFIX)
            encodeDefault = XmlEncodeDefault.ANNOTATED
            throwOnRepeatedElement = true
            isInlineCollapsedDefault = true
            isStrictAttributeNames = true
            isStrictOtherAttributes = true
            isStrictBoolean = true
            isXmlFloat = true
        }

    }

    private companion object {

        @Deprecated("Replace with Builder10", ReplaceWith("Builder10()"))
        public fun Builder(): Builder10 = Builder10()
        @Deprecated("Replace with Builder10", ReplaceWith("Builder10(policy)"))
        public fun Builder(policy: DefaultXmlSerializationPolicy): Builder10 = Builder10(policy)
        @Deprecated("Replace with Builder10", ReplaceWith("Builder10(policy)"))
        public fun Builder(policy: XmlSerializationPolicy): Builder10 = Builder10(policy)

        val ILLEGALNAMES = arrayOf("xml", "xmlns")

        val RESTRICTED_PREFIXES = mapOf(
            XMLConstants.XML_NS_PREFIX to XMLConstants.XML_NS_URI,
            XMLConstants.XMLNS_ATTRIBUTE to XMLConstants.XMLNS_ATTRIBUTE_NS_URI
        )
    }

}
