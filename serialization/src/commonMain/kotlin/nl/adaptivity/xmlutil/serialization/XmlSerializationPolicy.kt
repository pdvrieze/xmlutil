/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.core.impl.multiplatform.computeIfAbsent
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.DeclaredNameInfo
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.XmlEncodeDefault
import nl.adaptivity.xmlutil.serialization.structure.*

/**
 * Policies allow for customizing the behaviour of the xml serialization
 */
@OptIn(ExperimentalSubclassOptIn::class)
@SubclassOptInRequired(ExperimentalXmlUtilApi::class)
public interface XmlSerializationPolicy {

    public val defaultPrimitiveOutputKind: OutputKind get() = OutputKind.Attribute
    public val defaultObjectOutputKind: OutputKind get() = OutputKind.Element

    @Deprecated("Use isStrictAttributeNames instead")
    public val isStrictNames: Boolean get() = false
    @Suppress("DEPRECATION")
    public val isStrictAttributeNames: Boolean get() = isStrictNames
    public val isStrictBoolean: Boolean get() = false
    public val isStrictOtherAttributes: Boolean get() = false

    @ExperimentalXmlUtilApi
    public val verifyElementOrder: Boolean get() = false

    @OptIn(ExperimentalSerializationApi::class)
    @ExperimentalXmlUtilApi
    public fun defaultOutputKind(serialKind: SerialKind): OutputKind =
        when (serialKind) {
            SerialKind.ENUM,
            StructureKind.OBJECT -> defaultObjectOutputKind

            is PrimitiveKind -> defaultPrimitiveOutputKind
            PolymorphicKind.OPEN -> OutputKind.Element
            else -> OutputKind.Element
        }

    public fun invalidOutputKind(message: String): Unit = ignoredSerialInfo(message)

    public fun ignoredSerialInfo(message: String)

    public fun effectiveName(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        outputKind: OutputKind,
        useName: DeclaredNameInfo = tagParent.elementUseNameInfo
    ): QName

    public fun isListEluded(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
    ): Boolean

    public fun isTransparentPolymorphic(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
    ): Boolean

    public fun polymorphicDiscriminatorName(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): QName?

    @Suppress("DEPRECATION")
    public fun serialTypeNameToQName(
        typeNameInfo: DeclaredNameInfo,
        parentNamespace: Namespace
    ): QName =
        serialNameToQName(typeNameInfo.serialName, parentNamespace)

    @Suppress("DEPRECATION")
    public fun serialUseNameToQName(
        useNameInfo: DeclaredNameInfo,
        parentNamespace: Namespace
    ): QName =
        serialNameToQName(useNameInfo.serialName, parentNamespace)

    @Deprecated("It is recommended to override serialTypeNameToQName and serialUseNameToQName instead")
    public fun serialNameToQName(
        serialName: String,
        parentNamespace: Namespace
    ): QName

    /**
     * Class holding the name information for either an attribute or type
     *
     * @property serialName The serialName as provided by the descriptor (element name for attribute,
     *     type name for type)
     * @property annotatedName The name provided through the `@XmlSerialName` annotation. The default
     *     policy always prioritises this over local names.
     * @property isDefaultNamespace For attribute values, determines whether the attribute would be
     *     in the default namespace. This allows for `@XmlSerialName` annotations that do not explicitly
     *     specify the name. It records whether the namespace attribute is the "unset"/default value in
     *     the annotation.
     */
    public data class DeclaredNameInfo(
        val serialName: String,
        val annotatedName: QName?,
        val isDefaultNamespace: Boolean/* = false*/
    ) {
        internal constructor(serialName: String) : this(serialName, null, false)
        internal constructor(name: QName): this(name.localPart, name, false)

        @OptIn(ExperimentalSerializationApi::class)
        internal constructor(descriptor: SerialDescriptor) : this(descriptor.serialName, (descriptor as? XmlSerialDescriptor)?.serialQName, false)

        init {
            check(!(isDefaultNamespace && annotatedName == null)) { "Default namespace requires there to be an annotated name" }
        }
    }

    public data class ActualNameInfo(
        val serialName: String,
        val annotatedName: QName
    )

    @Deprecated("Don't use or implement this, use the 3 parameter version")
    public fun effectiveOutputKind(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
    ): OutputKind

    public fun effectiveOutputKind(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        canBeAttribute: Boolean
    ): OutputKind {
        @Suppress("DEPRECATION")
        val base = effectiveOutputKind(serializerParent, tagParent)

        if (!canBeAttribute && base == OutputKind.Attribute) {
            return handleAttributeOrderConflict(
                serializerParent,
                tagParent,
                base
            )
        }
        return base
    }

    public fun overrideSerializerOrNull(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): KSerializer<*>? {
        return null
    }

    @ExperimentalXmlUtilApi
    @Suppress("DirectUseOfResultType", "DEPRECATION")
    public fun handleUnknownContentRecovering(
        input: XmlReader,
        inputKind: InputKind,
        descriptor: XmlDescriptor,
        name: QName?,
        candidates: Collection<Any>
    ): List<XML.ParsedData<*>> {
        handleUnknownContent(input, inputKind, name, candidates)
        return emptyList()
    }

    public fun onElementRepeated(parentDescriptor: XmlDescriptor, childIndex: Int) {}

    @Deprecated("Use the recoverable version that allows returning a value")
    public fun handleUnknownContent(
        input: XmlReader,
        inputKind: InputKind,
        name: QName?,
        candidates: Collection<Any>
    )

    public fun handleAttributeOrderConflict(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        outputKind: OutputKind
    ): OutputKind {
        throw SerializationException("Node ${serializerParent.elementUseNameInfo.serialName} wants to be an attribute but cannot due to ordering constraints")
    }

    public fun shouldEncodeElementDefault(elementDescriptor: XmlDescriptor?): Boolean

    /**
     * Allow modifying the ordering of children.
     */
    public fun initialChildReorderMap(
        parentDescriptor: SerialDescriptor
    ): Collection<XmlOrderConstraint>? = null

    public fun updateReorderMap(
        original: Collection<XmlOrderConstraint>,
        children: List<XmlDescriptor>
    ): Collection<XmlOrderConstraint> = original

    @OptIn(ExperimentalSerializationApi::class)
    public fun enumEncoding(enumDescriptor: SerialDescriptor, index: Int): String {
        return enumDescriptor.getElementName(index)
    }

    @ExperimentalXmlUtilApi
    public fun preserveSpace(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): TypePreserveSpace {
        return tagParent.descriptor?.defaultPreserveSpace ?: TypePreserveSpace.DEFAULT
    }

    /** Determine the name of map keys for a given map type */
    public fun mapKeyName(serializerParent: SafeParentInfo): DeclaredNameInfo =
        DeclaredNameInfo("key") // minimal default for implementations.

    /**
     * Determine the name of the values for a given map type
     */
    public fun mapValueName(serializerParent: SafeParentInfo, isListEluded: Boolean): DeclaredNameInfo =
        DeclaredNameInfo("value") // minimal default for implementations.

    /**
     * Determine the name to use for the map element (only used when a map entry is wrapped)
     */
    public fun mapEntryName(serializerParent: SafeParentInfo, isListEluded: Boolean): QName =
        QName(serializerParent.namespace.namespaceURI, "entry") // minimal default for implementations.

    /**
     * Determine whether the key attribute should be collapsed into the value tag rather
     * than the value being nested in a container for the element.
     */
    public fun isMapValueCollapsed(mapParent: SafeParentInfo, valueDescriptor: XmlDescriptor): Boolean = false

    /**
     * Determine namespace prefixes to make sure are set upon the tag.
     */
    @ExperimentalXmlUtilApi
    public fun elementNamespaceDecls(serializerParent: SafeParentInfo): List<Namespace> = emptyList()

    /**
     * Determine the delimiters to use to separate attributes. When writing will always write the
     * first element.
     */
    @ExperimentalXmlUtilApi
    public fun attributeListDelimiters(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): Array<String> =
        arrayOf(" ", "\n", "\t", "\r")

    /**
     * Determine the delimiters to use to separate primitive/textual list elements when use inside an element.
     * When writing will always write the first element.
     */
    @ExperimentalXmlUtilApi
    public fun textListDelimiters(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): Array<String> =
        attributeListDelimiters(serializerParent, tagParent)

    public enum class XmlEncodeDefault {
        ALWAYS, ANNOTATED, NEVER
    }

    public companion object {

        /**
         * Helper function that allows more flexibility on null namespace use. If either the found
         * name has the null namespace, or the candidate has null namespace, this will map (for the
         * correct child).
         */
        @ExperimentalXmlUtilApi
        public fun recoverNullNamespaceUse(
            inputKind: InputKind,
            descriptor: XmlDescriptor,
            name: QName?
        ): List<XML.ParsedData<*>>? {
            if (name != null) {
                if (name.namespaceURI == "") {
                    for (idx in 0 until descriptor.elementsCount) {
                        val candidate = descriptor.getElementDescriptor(idx)
                        if (inputKind.mapsTo(candidate.effectiveOutputKind) &&
                            candidate.tagName.localPart == name.getLocalPart()
                        ) {
                            return listOf(XML.ParsedData(idx, Unit, true))
                        }
                    }
                } else {
                    for (idx in 0 until descriptor.elementsCount) {
                        val candidate = descriptor.getElementDescriptor(idx)
                        if (inputKind.mapsTo(candidate.effectiveOutputKind) &&
                            candidate.tagName.isEquivalent(QName(name.localPart))
                        ) {
                            return listOf(XML.ParsedData(idx, Unit, true))
                        }
                    }
                }
            }
            return null
        }

    }

}

public fun XmlSerializationPolicy.typeQName(xmlDescriptor: XmlDescriptor): QName {
    return xmlDescriptor.typeDescriptor.typeQname
        ?: serialTypeNameToQName(xmlDescriptor.typeDescriptor.typeNameInfo, xmlDescriptor.tagParent.namespace)
}

/**
 * Default implementation of a serialization policy that provides a behaviour that attempts to create an XML format
 * that resembles what would be created manually in line with XML's design.
 *
 * @property pedantic Enable some stricter behaviour
 * @property autoPolymorphic Should polymorphic information be retrieved using [SerializersModule] configuration. This replaces
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
 */
@OptIn(ExperimentalSubclassOptIn::class)
@SubclassOptInRequired(ExperimentalXmlUtilApi::class)
public open class DefaultXmlSerializationPolicy
private constructor(
    internal val formatCache: FormatCache,
    public val pedantic: Boolean,
    public val autoPolymorphic: Boolean,
    public val encodeDefault: XmlEncodeDefault,
    public val unknownChildHandler: UnknownChildHandler,
    public val typeDiscriminatorName: QName?,
    public val throwOnRepeatedElement: Boolean,
    public override val verifyElementOrder: Boolean,
    public override val isStrictAttributeNames: Boolean,
    public override val isStrictBoolean: Boolean,
    public override val isStrictOtherAttributes: Boolean,
) : XmlSerializationPolicy {

    @Deprecated("Invalid name of property. This only affects attributes")
    public override val isStrictNames: Boolean get() = isStrictAttributeNames

    @Deprecated("Use builder")
    @ExperimentalXmlUtilApi
    public constructor(
        pedantic: Boolean,
        autoPolymorphic: Boolean = false,
        encodeDefault: XmlEncodeDefault = XmlEncodeDefault.ANNOTATED,
        unknownChildHandler: UnknownChildHandler = XmlConfig.DEFAULT_UNKNOWN_CHILD_HANDLER,
        typeDiscriminatorName: QName? = null,
        throwOnRepeatedElement: Boolean = false,
        verifyElementOrder: Boolean = false,
    ) : this(
        formatCache = defaultSharedFormatCache(),
        pedantic = pedantic,
        autoPolymorphic = autoPolymorphic,
        encodeDefault = encodeDefault,
        unknownChildHandler = unknownChildHandler,
        typeDiscriminatorName = typeDiscriminatorName,
        throwOnRepeatedElement = throwOnRepeatedElement,
        verifyElementOrder = verifyElementOrder,
        isStrictAttributeNames = false,
        isStrictBoolean = false,
        isStrictOtherAttributes = false
    )

    @Suppress("DEPRECATION")
    @Deprecated("Use builder")
    @ExperimentalXmlUtilApi
    public constructor(
        pedantic: Boolean,
        typeDiscriminatorName: QName,
        encodeDefault: XmlEncodeDefault = XmlEncodeDefault.ANNOTATED,
        unknownChildHandler: UnknownChildHandler = XmlConfig.DEFAULT_UNKNOWN_CHILD_HANDLER,
        throwOnRepeatedElement: Boolean = false,
        verifyElementOrder: Boolean = false,
    ) : this(pedantic, false, encodeDefault, unknownChildHandler, typeDiscriminatorName, throwOnRepeatedElement, verifyElementOrder)

    /**
     * Stable constructor that doesn't use experimental api.
     */
    @Suppress("DEPRECATION")
    @Deprecated("Use builder")
    @OptIn(ExperimentalXmlUtilApi::class)
    public constructor(
        pedantic: Boolean,
        autoPolymorphic: Boolean = false,
        encodeDefault: XmlEncodeDefault = XmlEncodeDefault.ANNOTATED,
    ) : this(pedantic, autoPolymorphic, encodeDefault, XmlConfig.DEFAULT_UNKNOWN_CHILD_HANDLER)

    @Suppress("DEPRECATION")
    @Deprecated("Use the unknownChildHandler version that allows for recovery")
    @ExperimentalXmlUtilApi
    public constructor(
        pedantic: Boolean,
        autoPolymorphic: Boolean = false,
        encodeDefault: XmlEncodeDefault = XmlEncodeDefault.ANNOTATED,
        unknownChildHandler: NonRecoveryUnknownChildHandler
    ) : this(
        pedantic,
        autoPolymorphic,
        encodeDefault,
        UnknownChildHandler { input, inputKind, _, name, candidates ->
            unknownChildHandler(input, inputKind, name, candidates); emptyList()
        }
    )

    @Suppress("DEPRECATION")
    @Deprecated("Use the primary constructor that takes the recoverable handler")
    @ExperimentalXmlUtilApi
    public constructor(
        pedantic: Boolean,
        autoPolymorphic: Boolean = false,
        unknownChildHandler: NonRecoveryUnknownChildHandler
    ) : this(pedantic, autoPolymorphic, XmlEncodeDefault.ANNOTATED, unknownChildHandler)

    @OptIn(ExperimentalXmlUtilApi::class)
    public constructor(original: XmlSerializationPolicy?) : this(
        formatCache = (original as? DefaultXmlSerializationPolicy)?.formatCache?.copy() ?: defaultSharedFormatCache(),
        pedantic = (original as? DefaultXmlSerializationPolicy)?.pedantic ?: false,
        autoPolymorphic = (original as? DefaultXmlSerializationPolicy)?.autoPolymorphic ?: false,
        encodeDefault = (original as? DefaultXmlSerializationPolicy)?.encodeDefault ?: XmlEncodeDefault.ANNOTATED,
        unknownChildHandler = original?.let { orig -> // If there is an original, get from it
            (orig as? DefaultXmlSerializationPolicy)?.unknownChildHandler // take the existing one if present
                ?: UnknownChildHandler { input, inputKind, descriptor, name, candidates ->
                    orig.handleUnknownContentRecovering(input, inputKind, descriptor, name, candidates)
                }
        } ?: XmlConfig.DEFAULT_UNKNOWN_CHILD_HANDLER, // otherwise the default
        typeDiscriminatorName = (original as? DefaultXmlSerializationPolicy)?.typeDiscriminatorName,
        throwOnRepeatedElement = (original as? DefaultXmlSerializationPolicy)?.throwOnRepeatedElement ?: false,
        verifyElementOrder = original?.verifyElementOrder ?: false,
        isStrictAttributeNames = original?.isStrictAttributeNames ?: false,
        isStrictBoolean = original?.isStrictBoolean ?: false,
        isStrictOtherAttributes = original?.isStrictOtherAttributes ?: false
    )

    @Suppress("DEPRECATION")
    @OptIn(ExperimentalXmlUtilApi::class)
    protected constructor(builder: Builder) : this(
        builder.formatCache,
        builder
    )

    @Deprecated("The builder now contains the format cache, so no need to use the multi-parameter version")
    @OptIn(ExperimentalXmlUtilApi::class)
    protected constructor(formatCache: FormatCache, builder: Builder): this(
        formatCache = formatCache,
        pedantic = builder.pedantic,
        autoPolymorphic = builder.autoPolymorphic,
        encodeDefault = builder.encodeDefault,
        unknownChildHandler = builder.unknownChildHandler,
        typeDiscriminatorName = builder.typeDiscriminatorName,
        throwOnRepeatedElement = builder.throwOnRepeatedElement,
        verifyElementOrder = builder.verifyElementOrder,
        isStrictAttributeNames = builder.isStrictAttributeNames,
        isStrictBoolean = builder.isStrictBoolean,
        isStrictOtherAttributes = builder.isStrictOtherAttributes
    )

    @Deprecated("Use/implement version that takes a FormatCache parameter")
    public constructor(config: Builder.() -> Unit) : this(defaultSharedFormatCache(),config)

    @Suppress("DEPRECATION")
    public constructor(formatCache: FormatCache, config: Builder.() -> Unit) : this(formatCache, Builder().apply(config))

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

    @Deprecated("Don't use or implement this, use the 3 parameter version")
    override fun effectiveOutputKind(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
    ): OutputKind {
        return effectiveOutputKind(serializerParent, tagParent, true)
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


    @Deprecated("It is recommended to override serialTypeNameToQName and serialUseNameToQName instead")
    override fun serialNameToQName(
        serialName: String,
        parentNamespace: Namespace
    ): QName {
        return when (serialName) {
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

            else -> serialName.substringAfterLast('.').toQname(parentNamespace)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun effectiveName(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        outputKind: OutputKind,
        useName: DeclaredNameInfo
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


            serialKind is PrimitiveKind ||
                    serialKind == StructureKind.MAP ||
                    serialKind == StructureKind.LIST ||
                    serialKind == PolymorphicKind.OPEN ||
                    typeNameInfo.serialName == "kotlin.Unit" || // Unit needs a special case
                    parentSerialKind is PolymorphicKind // child of explict polymorphic uses predefined names
            -> serialUseNameToQName(useName, parentNamespace)

            typeNameInfo.annotatedName != null -> typeNameInfo.annotatedName

            else -> serialTypeNameToQName(typeNameInfo, parentNamespace)
        }

        if (pedantic) {
            require(name.localPart !in ILLEGALNAMES) { "QNames may not have reserved names as value :${name.localPart}" }
            require(name.prefix !in ILLEGALNAMES) { "QNames may not have reserved names as prefix :${name.prefix}" }
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

    @Deprecated("Don't use anymore, use the version that allows for recovery")
    override fun handleUnknownContent(
        input: XmlReader,
        inputKind: InputKind,
        name: QName?,
        candidates: Collection<Any>
    ) {
        throw UnsupportedOperationException("this function should not be called")
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

    /**
     * Default implementation that uses [XmlBefore] and [XmlAfter]. It does
     * not use the parent descriptor at all.
     */
    @OptIn(ExperimentalSerializationApi::class)
    override fun initialChildReorderMap(
        parentDescriptor: SerialDescriptor
    ): Collection<XmlOrderConstraint>? {
        val mapCapacity = parentDescriptor.elementsCount*2
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

/*
    override fun updateReorderMap(
        original: Collection<XmlOrderConstraint>,
        children: List<XmlDescriptor>
    ): Collection<XmlOrderConstraint> {
        fun Int.isAttribute(): Boolean = children[this].effectiveOutputKind == OutputKind.Attribute

        return original.filter { constraint ->
            if (constraint.before == XmlOrderConstraint.OTHERS || constraint.after == XmlOrderConstraint.OTHERS) {
                true
            } else {
                val (isBeforeAttribute, isAfterAttribute) = constraint.map { it.isAttribute() }

                isBeforeAttribute || (!isAfterAttribute)
            }
        }
    }
*/

    @OptIn(ExperimentalSerializationApi::class)
    @ExperimentalXmlUtilApi
    override fun preserveSpace(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): TypePreserveSpace {
        val b = serializerParent.descriptor?.defaultPreserveSpace ?: TypePreserveSpace.DEFAULT

        return b.overrideIgnore(serializerParent.useAnnIgnoreWhitespace)
    }

    override fun mapKeyName(serializerParent: SafeParentInfo): DeclaredNameInfo {
        val an = serializerParent.useAnnKeyName
        if (an != null) {
            return DeclaredNameInfo(
                an.value,
                an.toQName(serializerParent.namespace),
                an.namespace == UNSET_ANNOTATION_VALUE
            )
        }

        return DeclaredNameInfo("key")
    }

    override fun mapValueName(serializerParent: SafeParentInfo, isListEluded: Boolean): DeclaredNameInfo {
        val childAnnotation = serializerParent.useAnnChildrenName
        val childrenName = childAnnotation?.toQName(serializerParent.namespace)
        return DeclaredNameInfo("value", childrenName, childAnnotation?.namespace == UNSET_ANNOTATION_VALUE)
    }

    override fun mapEntryName(serializerParent: SafeParentInfo, isListEluded: Boolean): QName {
        if (isListEluded) { // If we don't have list tags, use the list name, otherwise use the default
            serializerParent.elementUseNameInfo.annotatedName?.let { return it }
        }
        return QName(serializerParent.namespace.namespaceURI, "entry")
    }

    @Suppress("DEPRECATION")
    private val pseudoConfig = XmlConfig(XmlConfig.Builder(policy = this))

    @OptIn(ExperimentalSerializationApi::class)
    override fun isMapValueCollapsed(mapParent: SafeParentInfo, valueDescriptor: XmlDescriptor): Boolean {
        val keyDescriptor = mapParent.elementSerialDescriptor.getElementDescriptor(0)
        val keyUseName = mapKeyName(mapParent)

        val pseudoKeyParent =
            InjectedParentTag(0, XmlTypeDescriptor(pseudoConfig, keyDescriptor, mapParent.namespace), keyUseName, mapParent.namespace)
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
    public open fun builder(): Builder = Builder(this)

    /**
     * Create a copy of this configuration with the changes specified through the config parameter.
     */
    @ExperimentalXmlUtilApi
    public inline fun copy(config: Builder.() -> Unit): DefaultXmlSerializationPolicy {
        return builder().apply(config).build()
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Deprecated("Use the copy that uses the builder to configure changes")
    public fun copy(
        pedantic: Boolean = this.pedantic,
        autoPolymorphic: Boolean = this.autoPolymorphic,
        encodeDefault: XmlEncodeDefault = this.encodeDefault,
        typeDiscriminatorName: QName? = this.typeDiscriminatorName
    ): DefaultXmlSerializationPolicy {
        return copy {
            this.pedantic = pedantic
            this.autoPolymorphic = autoPolymorphic
            this.encodeDefault = encodeDefault
            this.typeDiscriminatorName = typeDiscriminatorName
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Deprecated("Use the copy that uses the builder to configure changes")
    @ExperimentalXmlUtilApi
    public fun copy(
        pedantic: Boolean = this.pedantic,
        autoPolymorphic: Boolean = this.autoPolymorphic,
        encodeDefault: XmlEncodeDefault = this.encodeDefault,
        unknownChildHandler: UnknownChildHandler,
        typeDiscriminatorName: QName? = this.typeDiscriminatorName
    ): DefaultXmlSerializationPolicy {
        return copy {
            this.pedantic = pedantic
            this.autoPolymorphic = autoPolymorphic
            this.encodeDefault = encodeDefault
            this.unknownChildHandler = unknownChildHandler
            this.typeDiscriminatorName = typeDiscriminatorName
        }
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
        result = 31 * result + isStrictOtherAttributes.hashCode()
        return result
    }


    /**
     * A configuration builder for the default serialization policy.
     *
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
     */
    @OptIn(ExperimentalXmlUtilApi::class)
    public open class Builder internal constructor(
        public var pedantic: Boolean,
        public var autoPolymorphic: Boolean,
        public var encodeDefault: XmlEncodeDefault,
        public var unknownChildHandler: UnknownChildHandler,
        public var typeDiscriminatorName: QName?,
        public var throwOnRepeatedElement: Boolean,
        public var verifyElementOrder: Boolean,
        public var isStrictAttributeNames: Boolean,
        public var isStrictBoolean: Boolean,
        public var isStrictOtherAttributes: Boolean,
        @ExperimentalXmlUtilApi
        public var formatCache: FormatCache,
    ) {
        /**
         * Constructor for default builder. To set any values, use the property setters. The primary constructor
         * is internal as it is not stable as new configuration options are added.
         */
        public constructor() : this(
            pedantic = false,
            autoPolymorphic = false,
            encodeDefault = XmlEncodeDefault.ANNOTATED,
            unknownChildHandler = XmlConfig.DEFAULT_UNKNOWN_CHILD_HANDLER,
            typeDiscriminatorName = null,
            throwOnRepeatedElement = false,
            verifyElementOrder = false,
            isStrictAttributeNames = false,
            isStrictBoolean = false,
            isStrictOtherAttributes = false,
            formatCache = try { defaultSharedFormatCache() } catch(e: Error) { FormatCache.Dummy }
        )

        @ExperimentalXmlUtilApi
        public constructor(policy: DefaultXmlSerializationPolicy) : this(
            pedantic = policy.pedantic,
            autoPolymorphic = policy.autoPolymorphic,
            encodeDefault = policy.encodeDefault,
            unknownChildHandler = policy.unknownChildHandler,
            typeDiscriminatorName = policy.typeDiscriminatorName,
            throwOnRepeatedElement = policy.throwOnRepeatedElement,
            verifyElementOrder = policy.verifyElementOrder,
            isStrictAttributeNames = policy.isStrictAttributeNames,
            isStrictBoolean = policy.isStrictOtherAttributes,
            isStrictOtherAttributes = policy.isStrictBoolean,
            formatCache = policy.formatCache.copy(),
        )

        public fun ignoreUnknownChildren() {
            unknownChildHandler = XmlConfig.IGNORING_UNKNOWN_CHILD_HANDLER
        }

        public fun ignoreNamespaces() {
            unknownChildHandler = XmlConfig.IGNORING_UNKNOWN_NAMESPACE_HANDLER
        }

        public fun build(): DefaultXmlSerializationPolicy {
            return DefaultXmlSerializationPolicy(this)
        }
    }

    private companion object {
        val ILLEGALNAMES = arrayOf("xml", "xmlns")
    }

}
