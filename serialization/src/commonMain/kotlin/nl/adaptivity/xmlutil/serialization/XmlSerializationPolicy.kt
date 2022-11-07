/*
 * Copyright (c) 2020.
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
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.DeclaredNameInfo
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.XmlEncodeDefault
import nl.adaptivity.xmlutil.serialization.impl.XmlQNameSerializer
import nl.adaptivity.xmlutil.serialization.structure.*


/**
 *
 */
public interface XmlSerializationPolicy {

    public val defaultPrimitiveOutputKind: OutputKind get() = OutputKind.Attribute
    public val defaultObjectOutputKind: OutputKind get() = OutputKind.Element

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

    public data class DeclaredNameInfo(
        val serialName: String,
        val annotatedName: QName?
    )

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
        candidates: Collection<PolyInfo>
    ): List<XML.ParsedData<*>> {
        handleUnknownContent(input, inputKind, name, candidates)
        return emptyList()
    }

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
    public fun preserveSpace(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): Boolean {
        return true
    }

    /** Determine the name of map keys for a given map type */
    public fun mapKeyName(serializerParent: SafeParentInfo): DeclaredNameInfo =
        DeclaredNameInfo("key", null) // minimal default for implementations.

    /**
     * Determine the name of the values for a given map type
     */
    public fun mapValueName(serializerParent: SafeParentInfo, isListEluded: Boolean): DeclaredNameInfo =
        DeclaredNameInfo("value", null) // minimal default for implementations.

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

    public enum class XmlEncodeDefault {
        ALWAYS, ANNOTATED, NEVER
    }

}

public fun XmlSerializationPolicy.typeQName(xmlDescriptor: XmlDescriptor): QName {
    return xmlDescriptor.typeDescriptor.typeQname
        ?: serialTypeNameToQName(xmlDescriptor.typeDescriptor.typeNameInfo, xmlDescriptor.tagParent.namespace)
}

/**
 * @param autoPolymorphic Should polymorphic information be retrieved using [SerializersModule] configuration. This replaces
 *                     [XmlPolyChildren], but changes serialization where that annotation is not applied. This option will
 *                     become the default in the future although XmlPolyChildren will retain precedence (when present)
 * @param unknownChildHandler A function that is called when an unknown child is found. By default an exception is thrown
 *                     but the function can silently ignore it as well.
 */
public open class DefaultXmlSerializationPolicy
@ExperimentalXmlUtilApi constructor(
    public val pedantic: Boolean,
    public val autoPolymorphic: Boolean = false,
    public val encodeDefault: XmlEncodeDefault = XmlEncodeDefault.ANNOTATED,
    private val unknownChildHandler: UnknownChildHandler,
    private val typeDiscriminatorName: QName? = null,
) : XmlSerializationPolicy {

    @ExperimentalXmlUtilApi
    public constructor(
        pedantic: Boolean,
        typeDiscriminatorName: QName,
        encodeDefault: XmlEncodeDefault = XmlEncodeDefault.ANNOTATED,
        unknownChildHandler: UnknownChildHandler = XmlConfig.DEFAULT_UNKNOWN_CHILD_HANDLER
    ) : this(pedantic, false, encodeDefault, unknownChildHandler, typeDiscriminatorName)

    /**
     * Stable constructor that doesn't use experimental api.
     */
    @OptIn(ExperimentalXmlUtilApi::class)
    public constructor(
        pedantic: Boolean,
        autoPolymorphic: Boolean = false,
        encodeDefault: XmlEncodeDefault = XmlEncodeDefault.ANNOTATED,
    ) : this(pedantic, autoPolymorphic, encodeDefault, XmlConfig.DEFAULT_UNKNOWN_CHILD_HANDLER)

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
        pedantic = (original as? DefaultXmlSerializationPolicy)?.pedantic ?: false,
        autoPolymorphic = (original as? DefaultXmlSerializationPolicy)?.pedantic ?: false,
        encodeDefault = (original as? DefaultXmlSerializationPolicy)?.encodeDefault ?: XmlEncodeDefault.ANNOTATED,
        unknownChildHandler = original?.let { orig -> // If there is an original, get from it
            (orig as? DefaultXmlSerializationPolicy)?.unknownChildHandler // take the existing one if present
                ?: UnknownChildHandler { input, inputKind, descriptor, name, candidates ->
                    orig.handleUnknownContentRecovering(input, inputKind, descriptor, name,
                        candidates as Collection<PolyInfo>
                    )
                }
        } ?: XmlConfig.DEFAULT_UNKNOWN_CHILD_HANDLER, // otherwise the default
        typeDiscriminatorName = (original as? DefaultXmlSerializationPolicy)?.typeDiscriminatorName,
    )

    override fun polymorphicDiscriminatorName(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): QName? {
        return typeDiscriminatorName
    }

    override fun isListEluded(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
    ): Boolean {
        val useAnnotations = tagParent.elementUseAnnotations
        val isMixed = useAnnotations.firstOrNull<XmlValue>()?.value == true
        if (isMixed) return true

        val reqChildrenName =
            useAnnotations.firstOrNull<XmlChildrenName>()?.toQName()
        return reqChildrenName == null
    }

    override fun isTransparentPolymorphic(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
    ): Boolean {
        val xmlPolyChildren =
            tagParent.elementUseAnnotations.firstOrNull<XmlPolyChildren>()
        return autoPolymorphic || xmlPolyChildren != null
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
            ?: serializerParent.elementSerialDescriptor

        return when (val overrideOutputKind =
            serializerParent.elementUseOutputKind) {
            null -> {
                val useAnnotations = tagParent.elementUseAnnotations
                val isValue =
                    useAnnotations.firstOrNull<XmlValue>()?.value == true
                var parentChildDesc = tagParent.elementSerialDescriptor
                while (parentChildDesc.isInline) {
                    parentChildDesc =
                        parentChildDesc.getElementDescriptor(0)
                }
                val elementKind = parentChildDesc.kind
                // If we can't be an attribue
                when {
                    elementKind == StructureKind.CLASS
                    -> OutputKind.Element

                    isValue -> OutputKind.Mixed

                    !canBeAttribute && (tagParent.elementUseOutputKind == OutputKind.Attribute)
                    -> handleAttributeOrderConflict(serializerParent, tagParent, OutputKind.Attribute)

                    !canBeAttribute -> OutputKind.Element

                    else -> tagParent.elementUseOutputKind
                        ?: serialDescriptor.declOutputKind()
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
                        ?: serialDescriptor.declOutputKind()
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

        return when {
            useName.annotatedName != null -> useName.annotatedName

            outputKind == OutputKind.Attribute -> QName(useName.serialName) // Use non-prefix attributes by default

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
        candidates: Collection<PolyInfo>
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

    @OptIn(ExperimentalSerializationApi::class)
    override fun overrideSerializerOrNull(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
    ): KSerializer<*>? =
        when (serializerParent.elementSerialDescriptor.serialName) {
            "javax.xml.namespace.QName?",
            "javax.xml.namespace.QName" -> XmlQNameSerializer

            else -> null
        }

    /**
     * Default implementation that uses [XmlBefore] and [XmlAfter]. It does
     * not use the parent descriptor at all.
     */
    @OptIn(ExperimentalSerializationApi::class)
    override fun initialChildReorderMap(
        parentDescriptor: SerialDescriptor
    ): Collection<XmlOrderConstraint>? {
        val nameToIdx =
            (0 until parentDescriptor.elementsCount).associateBy {
                parentDescriptor.getElementName(it)
            }

        fun String.toChildIndex(): Int {
            return nameToIdx[this]
                ?: throw XmlSerialException("Could not find the attribute in ${parentDescriptor.serialName} with the name: $this\n  Candidates were: ${nameToIdx.keys.joinToString()}")
        }

        val orderConstraints = HashSet<XmlOrderConstraint>()
        val orderNodes = mutableMapOf<String, XmlOrderNode>()
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
                    val node = orderNodes.getOrPut(
                        parentDescriptor.getElementName(elementIdx)
                    ) {
                        XmlOrderNode(
                            elementIdx
                        )
                    }
                    if (xmlBefore != null) {
                        val befores = Array(xmlBefore.size) {
                            val name = xmlBefore[it]
                            orderNodes.getOrPut(name) { XmlOrderNode(name.toChildIndex()) }
                        }
                        node.addSuccessors(*befores)
                    }
                    if (xmlAfter != null) {
                        val afters = Array(xmlAfter.size) {
                            val name = xmlAfter[it]
                            orderNodes.getOrPut(name) { XmlOrderNode(name.toChildIndex()) }
                        }
                        node.addPredecessors(*afters)
                    }

                }
            }
        }
        if (orderNodes.isEmpty()) return null // no order nodes, no reordering

        return if (orderConstraints.isEmpty()) null else orderConstraints.toList()
    }

    override fun updateReorderMap(
        original: Collection<XmlOrderConstraint>,
        children: List<XmlDescriptor>
    ): Collection<XmlOrderConstraint> {

        fun Int.isAttribute(): Boolean = children[this].outputKind == OutputKind.Attribute

        return original.filter { constraint ->
            val (isBeforeAttribute, isAfterAttribute) = constraint.map(Int::isAttribute)

            isBeforeAttribute || (!isAfterAttribute)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @ExperimentalXmlUtilApi
    override fun preserveSpace(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): Boolean {
        serializerParent.elementUseAnnotations.firstOrNull<XmlIgnoreWhitespace>()?.apply { return !value }
        return !(serializerParent.elementSerialDescriptor.annotations
            .firstOrNull<XmlIgnoreWhitespace>()?.value ?: false)
    }

    override fun mapKeyName(serializerParent: SafeParentInfo): DeclaredNameInfo {
        return DeclaredNameInfo("key", null)
    }

    override fun mapValueName(serializerParent: SafeParentInfo, isListEluded: Boolean): DeclaredNameInfo {
        val childrenName = serializerParent.elementUseAnnotations.firstOrNull<XmlChildrenName>()?.toQName()
        return DeclaredNameInfo("value", childrenName)
    }

    override fun mapEntryName(serializerParent: SafeParentInfo, isListEluded: Boolean): QName {
        if (isListEluded) { // If we don't have list tags, use the list name, otherwise use the default
            serializerParent.elementUseNameInfo.annotatedName?.let { return it }
        }
        return QName(serializerParent.namespace.namespaceURI, "entry")
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun isMapValueCollapsed(mapParent: SafeParentInfo, valueDescriptor: XmlDescriptor): Boolean {
        val keyDescriptor = mapParent.elementSerialDescriptor.getElementDescriptor(0)
        val keyUseName = mapKeyName(mapParent)

        val pseudoKeyParent =
            InjectedParentTag(0, XmlTypeDescriptor(keyDescriptor, mapParent.namespace), keyUseName, mapParent.namespace)
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

    @ExperimentalXmlUtilApi
    override fun elementNamespaceDecls(serializerParent: SafeParentInfo): List<Namespace> {
        val annotations = (serializerParent.elementUseAnnotations.asSequence() +
                serializerParent.elementTypeDescriptor.serialDescriptor.annotations)
        return annotations
            .filterIsInstance<XmlNamespaceDeclSpec>()
            .flatMap { decl ->
                decl.namespaces
            }.toList()
    }

    override fun ignoredSerialInfo(message: String) {
        if (pedantic) throw XmlSerialException(message)
    }

    @OptIn(ExperimentalXmlUtilApi::class)
    public fun copy(
        pedantic: Boolean = this.pedantic,
        autoPolymorphic: Boolean = this.autoPolymorphic,
        encodeDefault: XmlEncodeDefault = this.encodeDefault,
        typeDiscriminatorName: QName? = this.typeDiscriminatorName
    ): DefaultXmlSerializationPolicy {
        return DefaultXmlSerializationPolicy(
            pedantic,
            autoPolymorphic,
            encodeDefault,
            unknownChildHandler,
            typeDiscriminatorName
        )
    }

    @ExperimentalXmlUtilApi
    public fun copy(
        pedantic: Boolean = this.pedantic,
        autoPolymorphic: Boolean = this.autoPolymorphic,
        encodeDefault: XmlEncodeDefault = this.encodeDefault,
        unknownChildHandler: UnknownChildHandler,
        typeDiscriminatorName: QName? = this.typeDiscriminatorName
    ): DefaultXmlSerializationPolicy {
        return DefaultXmlSerializationPolicy(
            pedantic,
            autoPolymorphic,
            encodeDefault,
            unknownChildHandler,
            typeDiscriminatorName
        )
    }
}
