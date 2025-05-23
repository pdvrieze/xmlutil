/*
 * Copyright (c) 2024.
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

@file:OptIn(WillBePrivate::class)
@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil.serialization.structure

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.impl.multiplatform.MpJvmDefaultWithCompatibility
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.XML.XmlCodecConfig
import nl.adaptivity.xmlutil.serialization.XmlCodecBase.Companion.declRequestedName
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.ActualNameInfo
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.DeclaredNameInfo
import nl.adaptivity.xmlutil.serialization.impl.OrderMatrix
import nl.adaptivity.xmlutil.serialization.impl.QNameMap
import nl.adaptivity.xmlutil.serialization.impl.maybeSerialName
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.CompactFragmentSerializer
import kotlin.reflect.KClass
import nl.adaptivity.xmlutil.serialization.CompactFragmentSerializer as DeprecatedCompactFragmentSerializer

/**
 * Interface describing a type without providing access to child xml descriptors
 */
@MpJvmDefaultWithCompatibility
public interface SafeXmlDescriptor {
    @ExperimentalSerializationApi
    public val isNullable: Boolean
        get() = serialDescriptor.isNullable

    @ExperimentalSerializationApi
    public val doInline: Boolean

    @ExperimentalSerializationApi
    public val kind: SerialKind
        get() = serialDescriptor.kind

    @ExperimentalXmlUtilApi
    public val defaultPreserveSpace: TypePreserveSpace

    public val typeDescriptor: XmlTypeDescriptor

    public val serializerParent: SafeParentInfo
    public val tagParent: SafeParentInfo
    public val tagName: QName
    public val serialDescriptor: SerialDescriptor
    public val outputKind: OutputKind
    public val elementsCount: Int
    public val overriddenSerializer: KSerializer<*>?

    public val isCData: Boolean get() = false

    @ExperimentalSerializationApi
    public val serialKind: SerialKind

    @ExperimentalSerializationApi
    public fun isElementOptional(index: Int): Boolean =
        serialDescriptor.isElementOptional(index)

}

@ExperimentalXmlUtilApi
public enum class TypePreserveSpace {
    /** The default preserve for the type */
    DEFAULT {
        override fun withDefault(d: Boolean): Boolean = d
    },
    /** Preserve whitespace, specifie as type default, also in children (where relevant) */
    PRESERVE {
        override fun withDefault(d: Boolean): Boolean = true
    },
    /** Forcibly ignore the whitespace */
    IGNORE {
        override fun withDefault(d: Boolean): Boolean = false
    };

    public abstract fun withDefault(d: Boolean): Boolean

    public fun overrideIgnore(annotatedIgnore: Boolean?): TypePreserveSpace = when (annotatedIgnore) {
        null -> this
        true -> IGNORE
        false -> PRESERVE
    }
}

@ExperimentalXmlUtilApi
public enum class DocumentPreserveSpace {
    /** The default preserve for the type */
    DEFAULT {
        override fun withDefault(d: Boolean): Boolean = d
    },
    /** Preserve whitespace, specifie as type default, also in children (where relevant) */
    DEFAULT_PRESERVE {
        override fun withDefault(d: Boolean): Boolean = true
    },
    /** Preserve whitespace, also in children (where relevant) */
    DOCUMENT_PRESERVE {
        override fun withDefault(d: Boolean): Boolean = true
        override fun withDefault(t: TypePreserveSpace): DocumentPreserveSpace = this
    },
    /** Forcibly ignore the whitespace */
    DEFAULT_IGNORE {
        override fun withDefault(d: Boolean): Boolean = false
    };

    public abstract fun withDefault(d: Boolean): Boolean

    public open fun withDefault(t: TypePreserveSpace): DocumentPreserveSpace = when (t) {
        TypePreserveSpace.DEFAULT -> this
        TypePreserveSpace.PRESERVE -> DEFAULT_PRESERVE
        TypePreserveSpace.IGNORE -> DEFAULT_IGNORE
    }
}

public sealed class XmlDescriptor(
    codecConfig: XmlCodecConfig,
    override val serializerParent: SafeParentInfo,
    final override val tagParent: SafeParentInfo = serializerParent,
) : SafeXmlDescriptor, Iterable<XmlDescriptor> {

    /**
     * Does this value represent an xml ID attribute (requiring global uniqueness)
     */
    public abstract val isIdAttr: Boolean

    public open val effectiveOutputKind: OutputKind
        get() = when (outputKind) {
            OutputKind.Inline -> getElementDescriptor(0).effectiveOutputKind
            else -> outputKind
        }

    final override val overriddenSerializer: KSerializer<*>? = serializerParent.overriddenSerializer

    protected val useNameInfo: DeclaredNameInfo = serializerParent.elementUseNameInfo

    override val typeDescriptor: XmlTypeDescriptor =
        serializerParent.elementTypeDescriptor

    public open val isUnsigned: Boolean get() = false

    @ExperimentalXmlUtilApi
    public val namespaceDecls: List<Namespace> =
        codecConfig.config.policy.elementNamespaceDecls(serializerParent)

    override val tagName: QName by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // not normalized to allow prefix to be preferred
        codecConfig.config.policy.effectiveName(serializerParent, tagParent, outputKind, useNameInfo)
    }

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

    override val serialDescriptor: SerialDescriptor get() = typeDescriptor.serialDescriptor

    @OptIn(ExperimentalSerializationApi::class)
    override val elementsCount: Int
        get() = typeDescriptor.serialDescriptor.elementsCount

    @ExperimentalSerializationApi
    override val serialKind: SerialKind
        get() = typeDescriptor.serialDescriptor.kind

    private val decoderProperties: DecoderProperties by lazy(LazyThreadSafetyMode.PUBLICATION) { DecoderProperties(codecConfig, this) }

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
    internal val visibleDescendantOrSelf by lazy(LazyThreadSafetyMode.PUBLICATION) { toNonTransparentChild() }

    public open fun getElementDescriptor(index: Int): XmlDescriptor {
        throw IndexOutOfBoundsException("There are no children")
    }

    @OptIn(ExperimentalSerializationApi::class)
    internal fun <A : Appendable> toString(builder: A, indent: Int, seen: MutableSet<String>): A {
        when (this) {
            is XmlContextualDescriptor,
            is XmlListDescriptor,
            is XmlPrimitiveDescriptor -> appendTo(builder, indent, seen)

            else -> if (serialDescriptor.serialName in seen) {
                builder.append(tagName.toString()).append("<...> = ").append(outputKind.name)
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

    private class DecoderProperties(codecConfig: XmlCodecConfig, xmlDescriptor: XmlDescriptor) {
        val polyMap: QNameMap<PolyInfo>
        val tagNameMap: QNameMap<Int>
        val attrMap: QNameMap<Int>
        val contextualChildren: IntArray

        init {
            val isUnchecked = codecConfig.config.isUnchecked

            val seenTagNames = HashSet<QName>()
            val localPolyMap = QNameMap<PolyInfo>()
            val localTagNameMap = QNameMap<Int>()
            val localAttrMap = QNameMap<Int>()
            val contextualChildren = mutableListOf<Int>()

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

                    elementDesc is XmlMapDescriptor && elementDesc.isListEluded && ! elementDesc.isValueCollapsed -> {
                        localTagNameMap[elementDesc.entryName.normalize()] = idx
                    }

                    elementDesc is XmlContextualDescriptor && xmlDescriptor.serialKind !is PolymorphicKind -> {
                        contextualChildren.add(idx)
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

            polyMap = localPolyMap
            tagNameMap = localTagNameMap
            attrMap = localAttrMap
            this.contextualChildren = contextualChildren.toIntArray()
        }
    }

    private inner class ElementIterator : Iterator<XmlDescriptor> {
        private var pos = 0

        override fun hasNext(): Boolean = pos < elementsCount

        override fun next(): XmlDescriptor = getElementDescriptor(pos++)
    }

    internal companion object {

        private fun XmlDescriptor.toNonTransparentChild(): XmlDescriptor {
            var result = this
            while (result is XmlInlineDescriptor || // Inline descriptors are only used when we actually elude the inline content
                (result is XmlListDescriptor && result.isListEluded)
            ) { // Lists may or may not be eluded

                result = result.getElementDescriptor(0)
            }
            if (result is XmlMapDescriptor && result.isListEluded) {
                if (result.isValueCollapsed) { // some transparent tags
                    return result.getElementDescriptor(1).toNonTransparentChild()
                }
            }
            return result
        }

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
                        codecConfig.config.isInlineCollapsed &&
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

public class XmlRootDescriptor internal constructor(
// TODO get rid of coded, put policy in its place
    codecConfig: XmlCodecConfig,
    descriptor: SerialDescriptor,
    tagName: DeclaredNameInfo,
) : XmlDescriptor(codecConfig, DetachedParent(codecConfig, descriptor, tagName, true)) {

    internal constructor(
        codecConfig: XmlCodecConfig,
        descriptor: SerialDescriptor,
    ) : this(codecConfig, descriptor, DeclaredNameInfo(descriptor))

    private val element: XmlDescriptor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        from(codecConfig, tagParent, canBeAttribute = false)
    }

    override val isIdAttr: Boolean get() = false

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = true // effectively a root descriptor is inline

    @ExperimentalXmlUtilApi
    override val defaultPreserveSpace: TypePreserveSpace
        get() = element.defaultPreserveSpace

    override val tagName: QName
        get() {
            val useNameInfo = useNameInfo
            return useNameInfo.annotatedName ?: element.tagName
        }

    override val outputKind: OutputKind get() = OutputKind.Mixed

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        if (index != 0) throw IndexOutOfBoundsException("There is exactly one child to a root tag")

        return element
    }

    override val elementsCount: Int get() = 1

    override fun appendTo(builder: Appendable, indent: Int, seen: MutableSet<String>) {
        builder.apply {
            append("<root>(")
            getElementDescriptor(0).appendTo(builder, indent + 4, seen)
            append(")")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlRootDescriptor

        return element == other.element
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + element.serialDescriptor.hashCode()
        return result
    }

}

public sealed class XmlValueDescriptor(
    codecConfig: XmlCodecConfig,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo
) : XmlDescriptor(codecConfig, serializerParent, tagParent) {

    public final override val isCData: Boolean = (serializerParent.useAnnCData
        ?: tagParent.useAnnCData
        ?: serializerParent.elementTypeDescriptor.typeAnnCData) == true


    @OptIn(ExperimentalXmlUtilApi::class)
    public val default: String? = tagParent.useAnnDefault

    private var defaultValue: Any? = UNSET

    @Deprecated("This is not safe anymore. This should have been internal.")
    @XmlUtilDeprecatedInternal
    public fun <T> defaultValue(deserializer: DeserializationStrategy<T>): T {
        val codec =
            XmlDecoderBase(getPlatformDefaultModule(), XmlConfig(), CompactFragment(default ?: "").getXmlReader())

        return defaultValue(codec, deserializer)
    }

    internal fun <T> defaultValue(
        xmlCodecBase: XmlCodecBase,
        deserializer: DeserializationStrategy<T>
    ): T {
        defaultValue.let { d ->
            @Suppress("UNCHECKED_CAST")
            if (d != UNSET) return d as T
        }

        @Suppress("UNCHECKED_CAST")
        if (default == null) return null as T

        val ps = DocumentPreserveSpace.DEFAULT.withDefault(defaultPreserveSpace)

        return when {
            effectiveOutputKind.let { it != OutputKind.Text && it != OutputKind.Text } ->
                defaultValue(xmlCodecBase.serializersModule, xmlCodecBase.config, deserializer)

            xmlCodecBase is XmlDecoderBase ->
                deserializer.deserialize(xmlCodecBase.StringDecoder(this, XmlReader.ExtLocationInfo(0, 0, 0), default, ps))

            else -> xmlCodecBase.run {
                val dec = XmlDecoderBase(serializersModule, config, CompactFragment("").getXmlReader())
                    .StringDecoder(this@XmlValueDescriptor, XmlReader.ExtLocationInfo(0, 0, 0), default, ps)

                deserializer.deserialize(dec)
            }
        }
    }

    private fun <T> defaultValue(
        serializersModule: SerializersModule,
        config: XmlConfig,
        deserializer: DeserializationStrategy<T>
    ): T {
        defaultValue.let { d ->
            @Suppress("UNCHECKED_CAST")
            if (d != UNSET) return d as T
        }
        val ps = DocumentPreserveSpace.DEFAULT.withDefault(defaultPreserveSpace)
        val d = when {
            default == null -> null

            effectiveOutputKind.let { it == OutputKind.Attribute || it == OutputKind.Text } -> {
                val xmlDecoderBase =
                    XmlDecoderBase(serializersModule, config, CompactFragment(default).getXmlReader())
                val dec = xmlDecoderBase.StringDecoder(this, XmlReader.ExtLocationInfo(0, 0, 0), default, ps)
                deserializer.deserialize(dec)
            }

            else -> {
                val defaultDecoder =
                    XmlDecoderBase(
                        serializersModule,
                        config,
                        CompactFragment(default).getXmlReader()
                    ).XmlDecoder(this, inheritedPreserveWhitespace = ps)
                deserializer.deserialize(defaultDecoder)
            }
        }
        defaultValue = d
        @Suppress("UNCHECKED_CAST")
        return d as T
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlValueDescriptor

        if (isCData != other.isCData) return false
        if (default != other.default) return false
        return defaultValue == other.defaultValue
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + isCData.hashCode()
        result = 31 * result + (default?.hashCode() ?: 0)
        result = 31 * result + (defaultValue?.hashCode() ?: 0)
        return result
    }

    private object UNSET
}

public class XmlPrimitiveDescriptor @ExperimentalXmlUtilApi
internal constructor(
    codecConfig: XmlCodecConfig,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
    canBeAttribute: Boolean,
    @ExperimentalXmlUtilApi override val defaultPreserveSpace: TypePreserveSpace
) : XmlValueDescriptor(codecConfig, serializerParent, tagParent) {

    override val isIdAttr: Boolean = serializerParent.useAnnIsId

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = false

    override val outputKind: OutputKind =
        codecConfig.config.policy.effectiveOutputKind(serializerParent, tagParent, canBeAttribute)

    override val elementsCount: Int get() = 0

    @OptIn(ExperimentalSerializationApi::class)
    override fun appendTo(builder: Appendable, indent: Int, seen: MutableSet<String>) {
        builder.append(tagName.toString())
            .append(':')
            .append(kind.toString())
            .append(" = ")
            .append(outputKind.toString())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlPrimitiveDescriptor

        return outputKind == other.outputKind
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + outputKind.hashCode()
        return result
    }

}

public class XmlInlineDescriptor internal constructor(
    codecConfig: XmlCodecConfig,
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

        val effectiveUseNameInfo: DeclaredNameInfo = when {
            useNameInfo.annotatedName != null -> useNameInfo

            typeDescriptor.typeNameInfo.annotatedName != null -> typeDescriptor.typeNameInfo

            else -> {
                // This is needed as this descriptor is not complete yet and would use this element's
                // unset name for the namespace.
                val serialName = typeDescriptor.serialDescriptor.getElementName(0)
                val annotation = typeDescriptor.serialDescriptor.getElementAnnotations(0).firstOrNull<XmlSerialName>()
                val qName = annotation?.toQName(serialName, tagParent.namespace)
                val childUseNameInfo =
                    DeclaredNameInfo(serialName, qName, annotation?.namespace == UNSET_ANNOTATION_VALUE)

                when {
                    childUseNameInfo.annotatedName != null -> childUseNameInfo

                    else -> useNameInfo
                }

            }
        }

        val useParentInfo = ParentInfo(codecConfig.config, this, 0, effectiveUseNameInfo)

        from(codecConfig, useParentInfo, tagParent, canBeAttribute)
    }

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        if (index != 0) throw IllegalArgumentException("Inline classes only have one child")
        return child
    }

    override val isUnsigned: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        serialDescriptor in UNSIGNED_SERIALIZER_DESCRIPTORS || child.isUnsigned
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
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return 31 * super.hashCode() + 137
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

public class XmlAttributeMapDescriptor internal constructor(
    codecConfig: XmlCodecConfig,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
    @ExperimentalXmlUtilApi
    override val defaultPreserveSpace: TypePreserveSpace
) : XmlValueDescriptor(codecConfig, serializerParent, tagParent) {
    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = false

    override val isIdAttr: Boolean get() = false

    override val outputKind: OutputKind get() = OutputKind.Attribute

    /**
     * The descriptor for the key type of the map
     */
    @Suppress("MemberVisibilityCanBePrivate")
    public val keyDescriptor: XmlDescriptor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        from(
            codecConfig,
            ParentInfo(codecConfig.config, this, 0, useOutputKind = OutputKind.Text),
            tagParent,
            true,
        )
    }

    /**
     * The descriptor for the value type of the map
     */
    @Suppress("MemberVisibilityCanBePrivate")
    public val valueDescriptor: XmlDescriptor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        from(
            codecConfig,
            ParentInfo(codecConfig.config, this, 1, useOutputKind = OutputKind.Text),
            tagParent,
            true
        )
    }

    override val elementsCount: Int get() = 2

    override fun getElementDescriptor(index: Int): XmlDescriptor = when (index % 2) {
        0 -> keyDescriptor
        else -> valueDescriptor
    }

    override fun appendTo(builder: Appendable, indent: Int, seen: MutableSet<String>) {
        builder.apply {
            append(tagName.toString())
                .appendLine(" (")
            appendIndent(indent)
            keyDescriptor.toString(this, indent + 4, seen)
                .appendLine(",")
            appendIndent(indent)
            valueDescriptor.toString(this, indent + 4, seen)
                .append(')')
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlAttributeMapDescriptor

        if (defaultPreserveSpace != other.defaultPreserveSpace) return false
        if (keyDescriptor != other.keyDescriptor) return false
        if (valueDescriptor != other.valueDescriptor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + defaultPreserveSpace.hashCode()
        result = 31 * result + keyDescriptor.hashCode()
        result = 31 * result + valueDescriptor.hashCode()
        return result
    }

}

public class XmlContextualDescriptor @ExperimentalXmlUtilApi
internal constructor(
    codecConfig: XmlCodecConfig,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
    private val canBeAttribute: Boolean,
    @ExperimentalXmlUtilApi
    override val defaultPreserveSpace: TypePreserveSpace,
) : XmlDescriptor(codecConfig, serializerParent, tagParent) {
    @ExperimentalSerializationApi
    override val doInline: Boolean get() = false

    override val isIdAttr: Boolean get() = false

    override val elementsCount: Int get() = 0

    @OptIn(ExperimentalSerializationApi::class)
    public val context: KClass<*>? = serializerParent.elementSerialDescriptor.capturedKClass

    override val effectiveOutputKind: OutputKind get() = outputKind

    override fun appendTo(builder: Appendable, indent: Int, seen: MutableSet<String>) {
        builder
            .append("CONTEXTUAL(")
            .append(tagParent.elementUseNameInfo.run { annotatedName?.toString() ?: serialName })
            .append(")")
    }

    internal fun resolve(
        codecConfig: XmlCodecConfig,
        descriptor: SerialDescriptor
    ): XmlDescriptor {
        val typeDescriptor = codecConfig.config.lookupTypeDesc(tagParent.namespace, descriptor)

        val overriddenParentInfo = DetachedParent(tagParent.namespace, typeDescriptor, useNameInfo)

        return from(codecConfig, overriddenParentInfo, tagParent, canBeAttribute)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlContextualDescriptor

        if (canBeAttribute != other.canBeAttribute) return false
        if (defaultPreserveSpace != other.defaultPreserveSpace) return false
        if (context != other.context) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + canBeAttribute.hashCode()
        result = 31 * result + defaultPreserveSpace.hashCode()
        result = 31 * result + (context?.hashCode() ?: 0)
        return result
    }

    override val outputKind: OutputKind get() = OutputKind.Inline
}

public class XmlCompositeDescriptor @ExperimentalXmlUtilApi
internal constructor(
    codecConfig: XmlCodecConfig,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
    @ExperimentalXmlUtilApi override val defaultPreserveSpace: TypePreserveSpace,
) : XmlValueDescriptor(codecConfig, serializerParent, tagParent) {

    init {
        val requestedOutputKind = codecConfig.config.policy.effectiveOutputKind(serializerParent, tagParent, false)
        when (requestedOutputKind) {
            OutputKind.Element -> {} // fine
            OutputKind.Mixed -> { // only the case for `@XmlValue` elements.
                // Permit this
            }
            else -> {
                codecConfig.config.policy.invalidOutputKind("Composite element: $tagName - Class SerialKinds/composites can only have Element output kinds, not $requestedOutputKind")
            }
        }
        if (requestedOutputKind != OutputKind.Element) {
        }
    }

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

    private val _lazyProps: Lazy<LazyProps> = lazy {
        when (val roInfo = initialChildReorderInfo) {
            null -> createElementDescriptors(codecConfig)
            else -> getReorderedElementDescriptors(codecConfig, roInfo)
        }
    }

    private val lazyProps: LazyProps get() = _lazyProps.value

    private val children: List<XmlDescriptor> get() = lazyProps.children

    private fun getReorderedElementDescriptors(
        codecConfig: XmlCodecConfig,
        initialChildReorderInfo: Collection<XmlOrderConstraint>
    ): LazyProps {
        val descriptors = arrayOfNulls<XmlDescriptor>(elementsCount)
        val policy = codecConfig.config.policy

        var attrMapChildIdx =
            if (policy.isStrictOtherAttributes) Int.MAX_VALUE else CompositeDecoder.UNKNOWN_NAME
        var valueChildIdx = CompositeDecoder.UNKNOWN_NAME


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
            children = children,
            attrMapChildIdx = if (attrMapChildIdx == Int.MAX_VALUE) CompositeDecoder.UNKNOWN_NAME else attrMapChildIdx,
            valueChildIdx = valueChildIdx,
            childReorderMap = childReorderInfo.second,
            childConstraints = childReorderInfo.first,
        )

    }

    private fun createElementDescriptors(
        codecConfig: XmlCodecConfig
    ): LazyProps {
        var valueChildIdx = CompositeDecoder.UNKNOWN_NAME
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

        return LazyProps(children, if (attrMapChildIdx == Int.MAX_VALUE) -1 else attrMapChildIdx, valueChildIdx)
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

        return initialChildReorderInfo == other.initialChildReorderInfo
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (initialChildReorderInfo?.hashCode() ?: 0)
        return result
    }

    private inner class LazyProps(
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
                        it != DeprecatedCompactFragmentSerializer.descriptor && it != CompactFragmentSerializer.descriptor
                    }
                ) {
                    val invalidIdx = children.indices
                        .firstOrNull { idx -> idx != valueChildIdx && children[idx].outputKind == OutputKind.Element }
                    if (invalidIdx != null) {
                        throw XmlSerialException(
                            "Types (${tagName}) with an @XmlValue member may not contain other child elements (${
                                serialDescriptor.getElementDescriptor(
                                    invalidIdx
                                )
                            }"
                        )
                    }
                }
            }

        }
    }
}

public sealed class PolymorphicMode {
    public data object TRANSPARENT : PolymorphicMode()
    public data object TAG : PolymorphicMode()
    public data class ATTR(public val name: QName) : PolymorphicMode()
}

@OptIn(ExperimentalSerializationApi::class)
public class XmlPolymorphicDescriptor internal constructor(
    private val codecConfig: XmlCodecConfig,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
    @ExperimentalXmlUtilApi
    override val defaultPreserveSpace: TypePreserveSpace,
) : XmlValueDescriptor(codecConfig, serializerParent, tagParent) {

    override val isIdAttr: Boolean
        get() = false

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = false

    override val outputKind: OutputKind

    public val polymorphicMode: PolymorphicMode
    public val isTransparent: Boolean get() = polymorphicMode == PolymorphicMode.TRANSPARENT
    public val polyInfo: Map<String, XmlDescriptor>
    public val typeQNameToSerialName: Map<QName, String>

    init {
        val policy = codecConfig.config.policy

        outputKind = policy.effectiveOutputKind(serializerParent, tagParent, canBeAttribute = false)

        val xmlPolyChildren = tagParent.useAnnPolyChildren

        // xmlPolyChildren and sealed also leads to a transparent polymorphic
        val polyAttrName = policy.polymorphicDiscriminatorName(serializerParent, tagParent)
        polymorphicMode = when {
            policy.isTransparentPolymorphic(serializerParent, tagParent) ->
                PolymorphicMode.TRANSPARENT

            polyAttrName == null -> PolymorphicMode.TAG
            else -> PolymorphicMode.ATTR(polyAttrName)
        }

        val localPolyInfo = HashMap<String, XmlDescriptor>()
        val localQNameToSerialName = HashMap<QName, String>()

        val wrapperUseName = when (polymorphicMode) {
            PolymorphicMode.TRANSPARENT -> null
            PolymorphicMode.TAG -> from(
                codecConfig,
                ParentInfo(codecConfig.config, this, 1), canBeAttribute = false
            ).tagName.let { DeclaredNameInfo(it) }

            is PolymorphicMode.ATTR -> DeclaredNameInfo(tagName)
        }


        when {
            // If the [XmlPolyChildren] annotation is present, use that
            xmlPolyChildren != null -> {
                val baseName = ActualNameInfo(
                    tagParent.descriptor?.serialDescriptor?.serialName ?: "",
                    tagParent.descriptor?.tagName ?: QName("", "")
                )
                val currentPkg = baseName.serialName.substringBeforeLast('.', "")

                val baseClass = serialDescriptor.capturedKClass ?: Any::class

                for (polyChild in xmlPolyChildren.value) {
                    val childInfo =
                        polyTagName(codecConfig, currentPkg, baseName, polyChild, baseClass)

                    val childSerializerParent = PolymorphicParentInfo(
                        parentDescriptor = this,
                        elementTypeDescriptor = childInfo.elementTypeDescriptor,
                        elementUseNameInfo = childInfo.useNameInfo,
                        elementUseOutputKind = OutputKind.Element,
                    )

                    val xmlDescriptor = from(codecConfig, childSerializerParent, tagParent, canBeAttribute = false)
                    localPolyInfo[childInfo.describedName] = xmlDescriptor
                    val qName = policy.typeQName(xmlDescriptor).normalize()
                    localQNameToSerialName[qName] = childInfo.describedName
                }
            }

            serialDescriptor.kind == PolymorphicKind.SEALED -> {
                // A sealed descriptor has 2 elements: 0 name: String, 1: value: elementDescriptor
                val d = serialDescriptor.getElementDescriptor(1)
                for (i in 0 until d.elementsCount) {
                    val childTypeDescriptor =
                        codecConfig.config.lookupTypeDesc(tagParent.namespace, d.getElementDescriptor(i))
                    val childDesc = d.getElementDescriptor(i)
                    val childSerializerParent = PolymorphicParentInfo(
                        parentDescriptor = this,
                        elementTypeDescriptor = childTypeDescriptor,
                        elementUseNameInfo = wrapperUseName ?: DeclaredNameInfo("value"),
                        elementUseOutputKind = outputKind,
                    )

                    val xmlDescriptor = from(codecConfig, childSerializerParent, tagParent, canBeAttribute = false)
                    var cd = xmlDescriptor
                    while (cd is XmlInlineDescriptor) { cd = cd.getElementDescriptor(0) }
                    val effectiveSerialName= if (cd.outputKind.isTextOrMixed) "kotlin.String" else childDesc.serialName

                    localPolyInfo[effectiveSerialName] = xmlDescriptor
                    val qName = policy.typeQName(xmlDescriptor).normalize()
                    localQNameToSerialName[qName] = childDesc.serialName

                }
            }

            else -> {

                val childDescriptors = codecConfig.serializersModule.getPolymorphicDescriptors(serialDescriptor)

                for (childDesc in childDescriptors) {
                    val childTypeDescriptor = codecConfig.config.lookupTypeDesc(tagParent.namespace, childDesc)

                    val childNameInfo = wrapperUseName ?: run {
                        tagName.toNamespace()
                        childTypeDescriptor.typeNameInfo//        @OptIn(ExperimentalSerializationApi::class)
                        //        return serialDescriptor.getNameInfo(config, parentNamespace, typeAnnXmlSerialName)
                    }

                    val childSerializerParent: SafeParentInfo = PolymorphicParentInfo(
                        parentDescriptor = this,
                        elementTypeDescriptor = childTypeDescriptor,
                        elementUseNameInfo = childNameInfo,
                        elementUseOutputKind = outputKind,
                    )

                    val xmlDescriptor = from(codecConfig, childSerializerParent, tagParent, canBeAttribute = false)

                    var cd = xmlDescriptor
                    while (cd is XmlInlineDescriptor) { cd = cd.getElementDescriptor(0) }
                    val effectiveSerialName= if (cd.outputKind.isTextOrMixed) "kotlin.String" else childDesc.serialName

                    localPolyInfo[effectiveSerialName] = xmlDescriptor

                    val qName = policy.typeQName(xmlDescriptor).normalize()
                    localQNameToSerialName[qName] = childDesc.serialName
                }
            }
        }

        polyInfo = localPolyInfo
        typeQNameToSerialName = localQNameToSerialName
    }

    @OptIn(ExperimentalSerializationApi::class)
    public val parentSerialName: String? get() =
        tagParent.descriptor?.serialDescriptor?.serialName ?: serialDescriptor.capturedKClass?.maybeSerialName

    @OptIn(WillBePrivate::class) // the type ParentInfo should become internal
    private val children by lazy(LazyThreadSafetyMode.PUBLICATION) {
        List(elementsCount) { index ->
            val canBeAttribute = index == 0
            val overrideOutputKind = if (canBeAttribute) OutputKind.Attribute else OutputKind.Element
            val parent = ParentInfo(codecConfig.config, this, index, useOutputKind = overrideOutputKind)

            from(codecConfig, parent, canBeAttribute = canBeAttribute)
        }
    }


    override fun getElementDescriptor(index: Int): XmlDescriptor {
        return children[index]
    }

    public fun getPolymorphicDescriptor(typeName: String): XmlDescriptor {
        polyInfo[typeName]?.let { return it }
        throw XmlSerialException("Missing polymorphic information for $typeName")
    }

    public fun getPolymorphicDescriptor(descriptor: SerialDescriptor): XmlDescriptor {
        polyInfo[descriptor.serialName]?.let { return it }

        val typeDescriptor = codecConfig.config.lookupTypeDesc(tagParent.namespace, descriptor)
        val overriddenParentInfo = DetachedParent(tagParent.namespace, typeDescriptor, useNameInfo)
        return from(codecConfig, overriddenParentInfo, tagParent, false)
    }

    override fun appendTo(builder: Appendable, indent: Int, seen: MutableSet<String>) {
        builder.apply {
            append(tagName.toString())
            when {
                isTransparent -> {
                    append(" <~(")
                    for (polyVal in polyInfo.values) {
                        polyVal.toString(this, indent + 4, seen).appendLine(',')
                    }
                }

                else -> {
                    append(" (")
                    append(" <poly> [")
                    for (polyVal in polyInfo.values) {
                        polyVal.toString(this, indent + 4, seen).appendLine(',')
                    }
                    append(']')
                }
            }
        }
    }

    @Suppress("DuplicatedCode")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlPolymorphicDescriptor

        if (outputKind != other.outputKind) return false
        if (polymorphicMode != other.polymorphicMode) return false
        if (polyInfo != other.polyInfo) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + outputKind.hashCode()
        result = 31 * result + polymorphicMode.hashCode()
        result = 31 * result + polyInfo.hashCode()
        return result
    }

    internal fun resolvePolymorphicTypeNameCandidates(name: QName, serializersModule: SerializersModule): List<String> {
        val baseClass = serialDescriptor.capturedKClass
        val directMatches = polyInfo.entries.mapNotNull { (typeName, xmlDesc) ->
            if (xmlDesc.tagName.isEquivalent(name)) return listOf(typeName)
            if (baseClass == null) {
                null
            } else {
                when (serializersModule.getPolymorphic(baseClass, typeName)) {
                    is XmlSerializer -> typeName
                    else -> null
                }
            }
        }
        if (directMatches.isNotEmpty() || baseClass == null) return directMatches
        val defaultMatch = serializersModule.getPolymorphic(baseClass, name.localPart)
        return listOfNotNull(defaultMatch?.descriptor?.serialName)
    }


}

/**
 * Determines the usage name information. As such it only looks at the property's serialName and XmlSerialName annotation.
 */
internal fun getElementNameInfo(
    serialName: String,
    parentNamespace: Namespace?,
    annotation: XmlSerialName?
): DeclaredNameInfo {
    val qName = annotation?.toQName(serialName, parentNamespace)
    return DeclaredNameInfo(serialName, qName, annotation?.namespace == UNSET_ANNOTATION_VALUE)
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

public sealed class XmlListLikeDescriptor(
    codecConfig: XmlCodecConfig,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo = serializerParent,
    @ExperimentalXmlUtilApi
    override val defaultPreserveSpace: TypePreserveSpace,
) : XmlDescriptor(codecConfig, serializerParent, tagParent) {

    public open val isListEluded: Boolean = when {
        tagParent is DetachedParent && tagParent.isDocumentRoot -> false
        else -> codecConfig.config.policy.isListEluded(serializerParent, tagParent)
    }

    @ExperimentalSerializationApi
    final override val doInline: Boolean get() = false

    @OptIn(ExperimentalSerializationApi::class, ExperimentalXmlUtilApi::class)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlListLikeDescriptor

        if (isListEluded != other.isListEluded) return false
        if (doInline != other.doInline) return false
        return defaultPreserveSpace == other.defaultPreserveSpace
    }

    @OptIn(ExperimentalXmlUtilApi::class, ExperimentalSerializationApi::class)
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + isListEluded.hashCode()
        result = 31 * result + doInline.hashCode()
        result = 31 * result + defaultPreserveSpace.hashCode()
        return result
    }

}

public class XmlMapDescriptor internal constructor(
    codecConfig: XmlCodecConfig,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
    preserveSpace: TypePreserveSpace
) : XmlListLikeDescriptor(codecConfig, serializerParent, tagParent, preserveSpace) {

    override val outputKind: OutputKind get() = OutputKind.Element

    override val isIdAttr: Boolean get() = false

    public val isValueCollapsed: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
        codecConfig.config.policy.isMapValueCollapsed(serializerParent, valueDescriptor)
    }

    internal val entryName: QName by lazy(LazyThreadSafetyMode.PUBLICATION) {
        when {
            isValueCollapsed -> valueDescriptor.tagName
            else -> codecConfig.config.policy.mapEntryName(serializerParent, isListEluded)
        }
    }

    private val keyDescriptor: XmlDescriptor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val keyNameInfo = codecConfig.config.policy.mapKeyName(serializerParent)
        val parentInfo = ParentInfo(codecConfig.config, this, 0, keyNameInfo)
        val keyTagParent = InjectedParentTag(0, typeDescriptor[0], keyNameInfo, tagParent.namespace)
        from(codecConfig, parentInfo, keyTagParent, canBeAttribute = true)
    }

    private val valueDescriptor: XmlDescriptor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val valueNameInfo = codecConfig.config.policy.mapValueName(serializerParent, isListEluded)
        val parentInfo = ParentInfo(codecConfig.config, this, 1, valueNameInfo, OutputKind.Element)
        val valueTagParent = InjectedParentTag(0, typeDescriptor[1], valueNameInfo, tagParent.namespace)
        from(codecConfig, parentInfo, valueTagParent, canBeAttribute = true)
    }

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        return when (index % 2) {
            0 -> keyDescriptor
            else -> valueDescriptor
        }
    }

    override fun appendTo(builder: Appendable, indent: Int, seen: MutableSet<String>) {
        builder.append(tagName.toString())
            .append(if (isListEluded) ": TransparentMap<" else ": ExplicitMap<")
        getElementDescriptor(0).appendTo(builder, indent + 4, seen)
        builder.append(", ")
        getElementDescriptor(1).appendTo(builder, indent + 4, seen)
        builder.append('>')
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlMapDescriptor

        if (isValueCollapsed != other.isValueCollapsed) return false
        if (entryName != other.entryName) return false
        if (keyDescriptor != other.keyDescriptor) return false
        if (valueDescriptor != other.valueDescriptor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + isValueCollapsed.hashCode()
        result = 31 * result + entryName.hashCode()
        result = 31 * result + keyDescriptor.hashCode()
        result = 31 * result + valueDescriptor.hashCode()
        return result
    }


}

public class XmlListDescriptor internal constructor(
    codecConfig: XmlCodecConfig,
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
                            DetachedParent(namespace, childTypeDescriptor, DeclaredNameInfo("item")),
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
            childrenNameAnnotation != null -> DeclaredNameInfo(
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

        if (isListEluded != other.isListEluded) return false
        return outputKind == other.outputKind
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + isListEluded.hashCode()
        result = 31 * result + outputKind.hashCode()
        result = 31 * result + serialDescriptor.getElementDescriptor(0).hashCode()
        return result
    }


}

/**
 * Interface that provides parent info that does provide actual access to the child. As such it is safe to
 * be used to determine properties of the child.
 */
@MpJvmDefaultWithCompatibility
public interface SafeParentInfo {
    /** Is the parent type an inline class. */
    public val parentIsInline: Boolean

    /** The index of this element in the parent. */
    public val index: Int

    /** The descriptor of the parent (if available - not for the root). */
    public val descriptor: SafeXmlDescriptor?

    /** The descriptor of the type of this element (independent of use). */
    public val elementTypeDescriptor: XmlTypeDescriptor

    /** The information on use site requirements */
    public val elementUseNameInfo: DeclaredNameInfo

    /** Annotations on the property, not type */
    public val elementUseAnnotations: Collection<Annotation>

    /** Value of the [XmlSerialName] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnXmlSerialName: XmlSerialName? get() = null

    /** Value of the [XmlElement] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnIsElement: Boolean? get() = null

    /** Value of the [XmlValue] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnIsValue: Boolean? get() = null

    /** Value of the [XmlPolyChildren] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnPolyChildren: XmlPolyChildren? get() = null

    /** Value of the [XmlIgnoreWhitespace] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnIgnoreWhitespace: Boolean? get() = null

    /** Value of the [XmlChildrenName] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnChildrenName: XmlChildrenName? get() = null

    /** Value of the [XmlKeyName] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnKeyName: XmlKeyName? get() = null

    /** Value of the [XmlKeyName] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnMapEntryName: XmlMapEntryName? get() = null

    /** Value of the [XmlCData] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnCData: Boolean? get() = null

    /** Value of the [XmlId] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnIsId: Boolean get() = false

    /** Value of the [XmlOtherAttributes] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnIsOtherAttributes: Boolean get() = false

    /** Value of the [XmlDefault] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnDefault: String? get() = null

    /** Value of the [XmlBefore] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnBefore: Array<out String>? get() = null

    /** Value of the [XmlAfter] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnAfter: Array<out String>? get() = null

    /** Value of the [XmlNamespaceDeclSpecs] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnNsDecls: List<Namespace>? get() = null

    /** The raw serial descriptor of the element*/
    public val elementSerialDescriptor: SerialDescriptor
        get() = elementTypeDescriptor.serialDescriptor

    /** Overidden serializer of the element*/
    public val overriddenSerializer: KSerializer<*>?

    /** Type requirements derived from the use site */
    public val elementUseOutputKind: OutputKind?

    /** The namespace this element has */
    public val namespace: Namespace

    public fun copy(
        config: XmlConfig,
        overriddenSerializer: KSerializer<*>? = this.overriddenSerializer
    ): SafeParentInfo

    public fun maybeOverrideSerializer(config: XmlConfig, overriddenSerializer: KSerializer<*>?): SafeParentInfo =
        when (overriddenSerializer) {
            null -> this
            else -> copy(config = config, overriddenSerializer = overriddenSerializer)
        }
}

internal class InjectedParentTag(
    override val index: Int,
    override val elementTypeDescriptor: XmlTypeDescriptor,
    override val elementUseNameInfo: DeclaredNameInfo,
    override val namespace: Namespace,
    override val elementUseOutputKind: OutputKind? = null,
    override val overriddenSerializer: KSerializer<*>? = null
) : SafeParentInfo {
    override val parentIsInline: Boolean get() = false

    override val descriptor: Nothing? get() = null

    override val elementUseAnnotations: Collection<Annotation>
        get() = emptyList()

    override fun copy(
        config: XmlConfig,
        overriddenSerializer: KSerializer<*>?
    ): InjectedParentTag {
        val newElementTypeDescriptor = overriddenSerializer?.let { config.lookupTypeDesc(namespace, it.descriptor) }
            ?: elementTypeDescriptor
        return InjectedParentTag(
            index,
            newElementTypeDescriptor,
            elementUseNameInfo,
            namespace,
            elementUseOutputKind,
            overriddenSerializer
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as InjectedParentTag

        if (index != other.index) return false
        if (elementTypeDescriptor != other.elementTypeDescriptor) return false
        if (elementUseNameInfo != other.elementUseNameInfo) return false
        if (namespace != other.namespace) return false
        if (elementUseOutputKind != other.elementUseOutputKind) return false
        if (overriddenSerializer != other.overriddenSerializer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + elementTypeDescriptor.hashCode()
        result = 31 * result + elementUseNameInfo.hashCode()
        result = 31 * result + namespace.hashCode()
        result = 31 * result + (elementUseOutputKind?.hashCode() ?: 0)
        result = 31 * result + (overriddenSerializer?.hashCode() ?: 0)
        return result
    }


}

private class DetachedParent(
    namespace: Namespace?,
    override val elementTypeDescriptor: XmlTypeDescriptor,
    override val elementUseNameInfo: DeclaredNameInfo,
    val isDocumentRoot: Boolean = false,
    override val elementUseOutputKind: OutputKind? = null,
    override val overriddenSerializer: KSerializer<*>? = null,
) : SafeParentInfo {

    constructor(
        codecConfig: XmlCodecConfig,
        serialDescriptor: SerialDescriptor,
        elementUseNameInfo: DeclaredNameInfo,
        isDocumentRoot: Boolean,
    ) : this(
        namespace = elementUseNameInfo.annotatedName?.toNamespace(),
        elementTypeDescriptor = (elementUseNameInfo.annotatedName?.toNamespace()
            ?: DEFAULT_NAMESPACE).let { namespace ->
            codecConfig.config.formatCache.lookupTypeOrStore(namespace, serialDescriptor) {
                XmlTypeDescriptor(codecConfig.config, serialDescriptor, namespace)
            }
        },
        elementUseNameInfo = elementUseNameInfo,
        isDocumentRoot = isDocumentRoot
    )

    override val namespace: Namespace = namespace ?: DEFAULT_NAMESPACE

    override fun copy(
        config: XmlConfig,
        overriddenSerializer: KSerializer<*>?,
    ): DetachedParent {
        val newElementTypeDescriptor = overriddenSerializer?.let { config.lookupTypeDesc(namespace, it.descriptor) }
            ?: elementTypeDescriptor

        return DetachedParent(namespace, newElementTypeDescriptor, elementUseNameInfo)
    }

    override val index: Int get() = -1

    override val descriptor: Nothing? get() = null

    override val parentIsInline: Boolean get() = false

    override val elementUseAnnotations: Collection<Annotation> get() = emptyList()

    @Suppress("DuplicatedCode")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DetachedParent

        if (elementTypeDescriptor != other.elementTypeDescriptor) return false
        if (elementUseNameInfo != other.elementUseNameInfo) return false
        if (isDocumentRoot != other.isDocumentRoot) return false
        if (overriddenSerializer != other.overriddenSerializer) return false
        if (elementUseOutputKind != other.elementUseOutputKind) return false

        return true
    }

    override fun hashCode(): Int {
        var result = elementTypeDescriptor.hashCode()
        result = 31 * result + elementUseNameInfo.hashCode()
        result = 31 * result + isDocumentRoot.hashCode()
        result = 31 * result + (overriddenSerializer?.hashCode() ?: 0)
        result = 31 * result + (elementUseOutputKind?.hashCode() ?: 0)
        return result
    }


}

internal val DEFAULT_NAMESPACE = XmlEvent.NamespaceImpl("", "")

private class PolymorphicParentInfo private constructor(
    override val descriptor: XmlPolymorphicDescriptor,
    override val namespace: Namespace,
    override val elementTypeDescriptor: XmlTypeDescriptor,
    override val elementUseNameInfo: DeclaredNameInfo,
    override val elementUseOutputKind: OutputKind? = null,
    override val elementUseAnnotations: Collection<Annotation>,
    override val overriddenSerializer: KSerializer<*>?,
) : SafeParentInfo {

    constructor(
        parentDescriptor: XmlPolymorphicDescriptor,
        elementTypeDescriptor: XmlTypeDescriptor,
        elementUseNameInfo: DeclaredNameInfo,
        elementUseOutputKind: OutputKind? = null,
        overriddenSerializer: KSerializer<*>? = null
    ): this (
        parentDescriptor,
        (elementUseNameInfo.annotatedName ?: parentDescriptor.tagName).toNamespace(),
        elementTypeDescriptor,
        elementUseNameInfo,
        elementUseOutputKind,
        parentDescriptor.serializerParent.elementUseAnnotations,
        overriddenSerializer
    )

    override val index: Int get() = -1 // no valid index
    override val parentIsInline: Boolean get() = false
    override fun copy(
        config: XmlConfig,
        overriddenSerializer: KSerializer<*>?
    ): SafeParentInfo {
        val newElementTypeDescriptor = overriddenSerializer?.let { config.lookupTypeDesc(namespace, it.descriptor) }
            ?: elementTypeDescriptor

        return PolymorphicParentInfo(
            descriptor,
            namespace,
            newElementTypeDescriptor,
            elementUseNameInfo,
            elementUseOutputKind,
            elementUseAnnotations,
            overriddenSerializer?: this.overriddenSerializer
        )
    }

}

@WillBePrivate // 2021-07-05 Should not have been public.
public class ParentInfo(
    config: XmlConfig,
    override val descriptor: XmlDescriptor,
    override val index: Int,
    useNameInfo: DeclaredNameInfo? = null,
    useOutputKind: OutputKind? = null,
    override val overriddenSerializer: KSerializer<*>? = null
) : SafeParentInfo {

    override fun copy(
        config: XmlConfig,
        overriddenSerializer: KSerializer<*>?
    ): ParentInfo {
        return ParentInfo(config, descriptor, index, elementUseNameInfo, elementUseOutputKind, overriddenSerializer)
    }

    @Suppress("DuplicatedCode")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ParentInfo

        if (index != other.index) return false
        if (overriddenSerializer != other.overriddenSerializer) return false
        if (elementUseNameInfo != other.elementUseNameInfo) return false
        if (elementUseOutputKind != other.elementUseOutputKind) return false
        return descriptor == other.descriptor
    }

    override fun hashCode(): Int {
        var result = descriptor.serialDescriptor.hashCode()
        result = 31 * result + index
        result = 31 * result + (overriddenSerializer?.hashCode() ?: 0)
        result = 31 * result + elementUseNameInfo.hashCode()
        result = 31 * result + (elementUseOutputKind?.hashCode() ?: 0)
        return result
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
                is XmlNamespaceDeclSpec -> useAnnNsDecls = an.namespaces
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
    override val elementUseNameInfo: DeclaredNameInfo = useNameInfo ?: when (index) {
        -1 -> DeclaredNameInfo(descriptor.serialDescriptor.serialName)
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
}

@ExperimentalXmlUtilApi
internal fun XmlTypeDescriptor.declOutputKind(): OutputKind? = when {
    typeAnnIsXmlValue == true -> OutputKind.Text
    typeAnnIsId -> OutputKind.Attribute
    typeAnnIsElement == true -> OutputKind.Element
    typeAnnIsElement == false -> OutputKind.Attribute
    typeAnnChildrenName != null || typeAnnPolyChildren != null -> OutputKind.Element
    else -> null
}

private fun polyTagName(
    codecConfig: XmlCodecConfig,
    currentPkg: String,
    parentName: ActualNameInfo,
    polyChildSpecification: String,
    baseClass: KClass<*>
): PolyBaseInfo {
    val serializersModule = codecConfig.serializersModule
    val config = codecConfig.config

    val parentTag = parentName.annotatedName

    val eqPos = polyChildSpecification.indexOf('=')
    val pkgPos: Int
    val typeNameBase: String
    val prefix: String
    val localPart: String

    if (eqPos < 0) {
        typeNameBase = polyChildSpecification
        pkgPos = polyChildSpecification.lastIndexOf('.')
        prefix = parentTag.prefix
        localPart = if (pkgPos < 0) polyChildSpecification else polyChildSpecification.substring(pkgPos + 1)
    } else {
        typeNameBase = polyChildSpecification.substring(0, eqPos).trim()
        pkgPos = polyChildSpecification.lastIndexOf('.', eqPos - 1)
        val prefPos = polyChildSpecification.indexOf(':', eqPos + 1)

        if (prefPos < 0) {
            prefix = parentTag.prefix
            localPart = polyChildSpecification.substring(eqPos + 1).trim()
        } else {
            prefix = polyChildSpecification.substring(eqPos + 1, prefPos).trim()
            localPart = polyChildSpecification.substring(prefPos + 1).trim()
        }
    }

    val typename = when {
        pkgPos != 0 || currentPkg.isEmpty() -> typeNameBase

        else -> "$currentPkg.${typeNameBase.substring(1)}"
    }

    val parentNamespace: XmlEvent.NamespaceImpl = XmlEvent.NamespaceImpl(prefix, parentTag.namespaceURI)

    @OptIn(ExperimentalSerializationApi::class)
    val descriptor = serializersModule.getPolymorphic(baseClass, typename)?.descriptor
        ?: throw XmlException("Missing descriptor for $typename in the serial context")


    val elementTypeDescriptor = config.formatCache.lookupTypeOrStore(parentNamespace, descriptor) {
        XmlTypeDescriptor(config, descriptor, parentNamespace)
    }

    val name: QName = when {
        eqPos < 0 -> {
            descriptor.declRequestedName(codecConfig, parentNamespace, elementTypeDescriptor.typeAnnXmlSerialName)
        }

        else -> QName(parentTag.namespaceURI, localPart, prefix)
    }
    return PolyBaseInfo(name, elementTypeDescriptor)
}

internal fun <A : Appendable> A.appendIndent(count: Int) = apply {
    for (i in 0 until count) {
        append(' ')
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.getXmlOverride() = when {
    this is XmlSerialDescriptor -> xmlDescriptor
    isNullable &&
            annotations.hasXmlSerialDesriptorMarker -> getElementDescriptor(-1).nullable

    else -> this
}

internal fun QName.normalize(): QName {
    return copy(prefix = "")
}

private val List<Annotation>.hasXmlSerialDesriptorMarker: Boolean
    get() {
        if (isEmpty()) return false
        return get(0) is XmlSerialDescriptorMarker
    }
