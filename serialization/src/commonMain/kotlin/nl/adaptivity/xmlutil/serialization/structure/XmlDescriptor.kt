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
import nl.adaptivity.xmlutil.serialization.XmlCodecBase.Companion.declRequestedName
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.ActualNameInfo
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.DeclaredNameInfo
import nl.adaptivity.xmlutil.serialization.impl.OrderMatrix
import nl.adaptivity.xmlutil.serialization.impl.maybeSerialName
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.CompactFragmentSerializer
import kotlin.reflect.KClass
import nl.adaptivity.xmlutil.serialization.CompactFragmentSerializer as DeprecatedCompactFragmentSerializer

@OptIn(ExperimentalSerializationApi::class)
@ExperimentalXmlUtilApi
internal val SerialDescriptor.declDefault: String?
    get() = annotations.declDefault

internal val Collection<Annotation>.declDefault: String?
    get() = firstOrNull<XmlDefault>()?.value

@OptIn(ExperimentalSerializationApi::class)
@ExperimentalXmlUtilApi
internal fun SerialDescriptor.declOutputKind(): OutputKind? {
    for (a in annotations) {
        when {
            a is XmlValue && a.value -> return OutputKind.Text
            a is XmlId -> return OutputKind.Attribute
            a is XmlElement -> return if (a.value) OutputKind.Element else OutputKind.Attribute
            a is XmlPolyChildren ||
                    a is XmlChildrenName -> return OutputKind.Element
        }
    }
    return null
}

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
    public val preserveSpace: Boolean

    public val typeDescriptor: XmlTypeDescriptor
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

public sealed class XmlDescriptor(
    policy: XmlSerializationPolicy,
    serializerParent: SafeParentInfo,
    final override val tagParent: SafeParentInfo = serializerParent,
) : SafeXmlDescriptor {

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
        policy.elementNamespaceDecls(serializerParent)

    override val tagName: QName by lazy {
        policy.effectiveName(serializerParent, tagParent, outputKind, useNameInfo)
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <V> effectiveSerializationStrategy(fallback: SerializationStrategy<V>): SerializationStrategy<V> {
        if (overriddenSerializer == (fallback as? KSerializer<Any>)?.nullable) return fallback
        return (overriddenSerializer ?: fallback) as SerializationStrategy<V>
    }

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
            config: XmlConfig,
            serializersModule: SerializersModule,
            serializerParent: SafeParentInfo,
            tagParent: SafeParentInfo = serializerParent,
            canBeAttribute: Boolean
        ): XmlDescriptor {
            val overridenSerializer = config.policy.overrideSerializerOrNull(serializerParent, tagParent)

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
                        config = config,
                        overriddenSerializer = overridenSerializer
                    )
                    effectiveTagParent = tagParent.copy(config = config, overriddenSerializer = overridenSerializer)
                }
            }

            val preserveSpace = config.policy.preserveSpace(serializerParent, tagParent)

            when (elementSerialDescriptor.kind) {
                SerialKind.ENUM,
                is PrimitiveKind ->
                    return XmlPrimitiveDescriptor(
                        config.policy,
                        effectiveSerializerParent,
                        effectiveTagParent,
                        canBeAttribute,
                        preserveSpace
                    )

                StructureKind.LIST ->
                    return XmlListDescriptor(
                        config,
                        serializersModule,
                        effectiveSerializerParent,
                        effectiveTagParent
                    )

                StructureKind.MAP -> {
                    return when {
                        serializerParent.useAnnIsOtherAttributes ->
                            XmlAttributeMapDescriptor(
                                config,
                                serializersModule,
                                effectiveSerializerParent,
                                effectiveTagParent
                            )

                        serializerParent.elementUseOutputKind == OutputKind.Attribute -> XmlAttributeMapDescriptor(
                            config,
                            serializersModule,
                            effectiveSerializerParent,
                            effectiveTagParent
                        )

                        else -> XmlMapDescriptor(
                            config,
                            serializersModule,
                            effectiveSerializerParent,
                            effectiveTagParent
                        )
                    }
                }

                is PolymorphicKind ->
                    return XmlPolymorphicDescriptor(
                        config,
                        serializersModule,
                        effectiveSerializerParent,
                        effectiveTagParent
                    )

                SerialKind.CONTEXTUAL ->
                    return XmlContextualDescriptor(
                        config,
                        effectiveSerializerParent,
                        effectiveTagParent,
                        canBeAttribute
                    )

                else -> {} // fall through to other handler.
            }

            return when {
                config.isInlineCollapsed &&
                        elementSerialDescriptor.isInline ->
                    XmlInlineDescriptor(
                        config,
                        serializersModule,
                        effectiveSerializerParent,
                        effectiveTagParent,
                        canBeAttribute
                    )

                else ->
                    XmlCompositeDescriptor(
                        config,
                        serializersModule,
                        effectiveSerializerParent,
                        effectiveTagParent,
                        preserveSpace
                    )
            }
        }

    }
}

public class XmlRootDescriptor internal constructor(
// TODO get rid of coded, put policy in its place
    config: XmlConfig,
    serializersModule: SerializersModule,
    descriptor: SerialDescriptor,
    tagName: DeclaredNameInfo,
) : XmlDescriptor(config.policy, DetachedParent(config, descriptor, tagName, true, outputKind = null)) {

    internal constructor(
// TODO get rid of coded, put policy in its place
        config: XmlConfig,
        serializersModule: SerializersModule,
        descriptor: SerialDescriptor,
    ) : this(config, serializersModule, descriptor, DeclaredNameInfo(descriptor))

    private val element: XmlDescriptor by lazy {
        from(config, serializersModule, tagParent, canBeAttribute = false)
    }

    override val isIdAttr: Boolean get() = false

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = true // effectively a root descriptor is inline

    @ExperimentalXmlUtilApi
    override val preserveSpace: Boolean
        get() = element.preserveSpace

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
        result = 31 * result + element.hashCode()
        return result
    }

}

public sealed class XmlValueDescriptor(
    policy: XmlSerializationPolicy,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo
) : XmlDescriptor(policy, serializerParent, tagParent) {

    @OptIn(ExperimentalSerializationApi::class)
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

        return when {
            effectiveOutputKind.let { it != OutputKind.Text && it != OutputKind.Text } ->
                defaultValue(xmlCodecBase.serializersModule, xmlCodecBase.config, deserializer)

            xmlCodecBase is XmlDecoderBase ->
                deserializer.deserialize(xmlCodecBase.StringDecoder(this, XmlReader.ExtLocationInfo(0, 0, 0), default))

            else -> xmlCodecBase.run {
                val dec = XmlDecoderBase(serializersModule, config, CompactFragment("").getXmlReader())
                    .StringDecoder(this@XmlValueDescriptor, XmlReader.ExtLocationInfo(0, 0, 0), default)

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
        val d = when {
            default == null -> null

            effectiveOutputKind.let { it == OutputKind.Attribute || it == OutputKind.Text } -> {
                val xmlDecoderBase: XmlDecoderBase =
                    XmlDecoderBase(serializersModule, config, CompactFragment(default).getXmlReader())
                val dec = xmlDecoderBase.StringDecoder(this, XmlReader.ExtLocationInfo(0, 0, 0), default)
                deserializer.deserialize(dec)
            }

            else -> {
                val defaultDecoder =
                    XmlDecoderBase(
                        serializersModule,
                        config,
                        CompactFragment(default).getXmlReader()
                    ).XmlDecoder(this)
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
    policy: XmlSerializationPolicy,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
    canBeAttribute: Boolean,
    @ExperimentalXmlUtilApi override val preserveSpace: Boolean
) : XmlValueDescriptor(policy, serializerParent, tagParent) {

    override val isIdAttr: Boolean = serializerParent.useAnnIsId

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = false

    override val outputKind: OutputKind =
        policy.effectiveOutputKind(serializerParent, tagParent, canBeAttribute)

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
    config: XmlConfig,
    serializersModule: SerializersModule,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
    canBeAttribute: Boolean
) : XmlValueDescriptor(config.policy, serializerParent, tagParent) {

    override val isIdAttr: Boolean = serializerParent.useAnnIsId

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = true

    @ExperimentalXmlUtilApi
    override val preserveSpace: Boolean
        get() = child.preserveSpace

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
    private val child: XmlDescriptor by lazy {

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

        val useParentInfo = ParentInfo(config, this, 0, effectiveUseNameInfo)

        from(config, serializersModule, useParentInfo, tagParent, canBeAttribute)
    }

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        if (index != 0) throw IllegalArgumentException("Inline classes only have one child")
        return child
    }

    override val isUnsigned: Boolean by lazy {
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
        if (!super.equals(other)) return false

        other as XmlInlineDescriptor

        return isUnsigned == other.isUnsigned
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + isUnsigned.hashCode()
        return result
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
    config: XmlConfig,
    serializersModule: SerializersModule,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo
) : XmlValueDescriptor(config.policy, serializerParent, tagParent) {
    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = false

    override val isIdAttr: Boolean get() = false

    @ExperimentalXmlUtilApi
    override val preserveSpace: Boolean
        get() = true

    override val outputKind: OutputKind get() = OutputKind.Attribute

    /**
     * The descriptor for the key type of the map
     */
    @Suppress("MemberVisibilityCanBePrivate")
    public val keyDescriptor: XmlDescriptor by lazy {
        from(
            config,
            serializersModule,
            ParentInfo(config, this, 0, useOutputKind = OutputKind.Text),
            tagParent,
            true,
        )
    }

    /**
     * The descriptor for the value type of the map
     */
    @Suppress("MemberVisibilityCanBePrivate")
    public val valueDescriptor: XmlDescriptor by lazy {
        from(
            config,
            serializersModule,
            ParentInfo(config, this, 1, useOutputKind = OutputKind.Text),
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

}

public class XmlContextualDescriptor @ExperimentalXmlUtilApi
internal constructor(
    config: XmlConfig,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
    private val canBeAttribute: Boolean
) : XmlDescriptor(config.policy, serializerParent, tagParent) {
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
        descriptor: SerialDescriptor,
        config: XmlConfig,
        serializersModule: SerializersModule
    ): XmlDescriptor {
        val overriddenParentInfo = DetachedParent(config, descriptor, useNameInfo, false)

        return from(config, serializersModule, overriddenParentInfo, tagParent, canBeAttribute)
    }

    @ExperimentalXmlUtilApi
    override val preserveSpace: Boolean get() = false

    override val outputKind: OutputKind get() = OutputKind.Inline
}

public class XmlCompositeDescriptor @ExperimentalXmlUtilApi
internal constructor(
    config: XmlConfig,
    serializersModule: SerializersModule,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
    @ExperimentalXmlUtilApi override val preserveSpace: Boolean,
) : XmlValueDescriptor(config.policy, serializerParent, tagParent) {

    init {
        val requestedOutputKind = config.policy.effectiveOutputKind(serializerParent, tagParent, false)
        if (requestedOutputKind != OutputKind.Element) {
            config.policy.invalidOutputKind("Class SerialKinds/composites can only have Element output kinds, not $requestedOutputKind")
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
    private val initialChildReorderInfo: Collection<XmlOrderConstraint>? get() = typeDescriptor.initialChildReorderInfo

    private val lazyProps: LazyProps by lazy {
        when (val roInfo = initialChildReorderInfo) {
            null -> getDefaultElementDescriptors(config, serializersModule)
            else -> getReorderedElementDescriptors(config, serializersModule, roInfo)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val children: List<XmlDescriptor> get() = lazyProps.children

    private fun getReorderedElementDescriptors(
        config: XmlConfig,
        serializersModule: SerializersModule,
        initialChildReorderInfo: Collection<XmlOrderConstraint>
    ): LazyProps {
        val descriptors = arrayOfNulls<XmlDescriptor>(elementsCount)
        var attrMapChildIdx =
            if (config.policy.isStrictOtherAttributes) Int.MAX_VALUE else CompositeDecoder.UNKNOWN_NAME
        var valueChildIdx = CompositeDecoder.UNKNOWN_NAME


        fun XmlOrderNode.ensureDescriptor(): XmlDescriptor {
            val elementIdx = this.elementIdx
            return descriptors[elementIdx] ?: let {
                val canBeAttribute =
                    if (predecessors.isEmpty()) true else predecessors.all { it.ensureDescriptor().outputKind == OutputKind.Attribute }

                val parentInfo = ParentInfo(config, this@XmlCompositeDescriptor, elementIdx)
                from(config, serializersModule, parentInfo, canBeAttribute = canBeAttribute).also { desc ->
                    descriptors[elementIdx] = desc
                    parentInfo.useAnnIsValue?.let { valueChildIdx = elementIdx }
                    if (parentInfo.useAnnIsOtherAttributes && desc is XmlAttributeMapDescriptor) {
                        attrMapChildIdx = elementIdx
                    } else if (attrMapChildIdx < 0 && config.policy.isStrictOtherAttributes && desc is XmlAttributeMapDescriptor) {
                        attrMapChildIdx = elementIdx
                    }

                }
            }
        }

        // sequence starts should be independent values that can be ordered in any way
        val sequenceStarts = initialChildReorderInfo.sequenceStarts(elementsCount)
        for (orderedSequence in sequenceStarts) {
            for (element in orderedSequence.flatten()) {
                element.ensureDescriptor()
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

    private fun getDefaultElementDescriptors(
        config: XmlConfig,
        serializersModule: SerializersModule
    ): LazyProps {
        var valueChildIdx = CompositeDecoder.UNKNOWN_NAME
        var attrMapChildIdx = if (config.policy.isStrictOtherAttributes) Int.MAX_VALUE else -1

        val children = List(serialDescriptor.elementsCount) { idx ->
            val parentInfo = ParentInfo(config, this, idx)
            val desc = from(config, serializersModule, parentInfo, canBeAttribute = true)

            if (parentInfo.useAnnIsValue == true) valueChildIdx = idx
            if (parentInfo.useAnnIsOtherAttributes && desc is XmlAttributeMapDescriptor) {
                attrMapChildIdx = idx
            } else if (attrMapChildIdx < 0 && config.policy.isStrictOtherAttributes && desc is XmlAttributeMapDescriptor) {
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
            for (child in children) {
                if (first) first = false else appendLine(',')
                appendIndent(indent)
                child.toString(this, indent + 4, seen)
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
        public val children: List<XmlDescriptor>,
        public val attrMapChildIdx: Int,
        public val valueChildIdx: Int,
        public val childReorderMap: IntArray? = null,
        public val childConstraints: OrderMatrix? = null,
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

public class XmlPolymorphicDescriptor internal constructor(
    config: XmlConfig,
    serializersModule: SerializersModule,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo
) : XmlValueDescriptor(config.policy, serializerParent, tagParent) {

    override val isIdAttr: Boolean
        get() = false

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = false

    @ExperimentalXmlUtilApi
    override val preserveSpace: Boolean
        get() = false

    override val outputKind: OutputKind =
        config.policy.effectiveOutputKind(serializerParent, tagParent, canBeAttribute = false)

    public val polymorphicMode: PolymorphicMode
    public val isTransparent: Boolean get() = polymorphicMode == PolymorphicMode.TRANSPARENT
    public val polyInfo: Map<String, XmlDescriptor>

    init {
        val xmlPolyChildren = tagParent.useAnnPolyChildren

        // xmlPolyChildren and sealed also leads to a transparent polymorphic
        val polyAttrName = config.policy.polymorphicDiscriminatorName(serializerParent, tagParent)
        polymorphicMode = when {
            config.policy.isTransparentPolymorphic(serializerParent, tagParent) ->
                PolymorphicMode.TRANSPARENT

            polyAttrName == null -> PolymorphicMode.TAG
            else -> PolymorphicMode.ATTR(polyAttrName)
        }

        @OptIn(ExperimentalSerializationApi::class)
        polyInfo = mutableMapOf<String, XmlDescriptor>().also { map ->

            val qName = when (polymorphicMode) {
                PolymorphicMode.TRANSPARENT -> null
                PolymorphicMode.TAG -> from(
                    config = config,
                    serializersModule = serializersModule, ParentInfo(config, this, 1), canBeAttribute = false
                ).tagName

                is PolymorphicMode.ATTR -> tagName
            }


            when {
                xmlPolyChildren != null -> {
                    val baseName = ActualNameInfo(
                        tagParent.descriptor?.serialDescriptor?.serialName ?: "",
                        tagParent.descriptor?.tagName ?: QName("", "")
                    )
                    val currentPkg = baseName.serialName.substringBeforeLast('.', "")

                    val baseClass = serialDescriptor.capturedKClass ?: Any::class

                    for (polyChild in xmlPolyChildren.value) {
                        val childInfo =
                            polyTagName(config, currentPkg, baseName, polyChild, baseClass, serializersModule)

                        val childSerializerParent =
                            DetachedParent(
                                childInfo.elementTypeDescriptor,
                                childInfo.useNameInfo,
                                false,
                                outputKind = OutputKind.Element,
                            )

                        map[childInfo.describedName] =
                            from(config, serializersModule, childSerializerParent, tagParent, canBeAttribute = false)
                    }
                }

                serialDescriptor.kind == PolymorphicKind.SEALED -> {
                    // A sealed descriptor has 2 elements: 0 name: String, 1: value: elementDescriptor
                    val d = serialDescriptor.getElementDescriptor(1)
                    for (i in 0 until d.elementsCount) {
                        val childDesc = d.getElementDescriptor(i)
                        val childSerializerParent = DetachedParent(
                            config,
                            childDesc,
                            qName,
                            false,
                            isDefaultNamespace = false
                        )

                        map[childDesc.serialName] =
                            from(config, serializersModule, childSerializerParent, tagParent, canBeAttribute = false)

                    }
                }

                else -> {

                    val childDescriptors = serializersModule.getPolymorphicDescriptors(serialDescriptor)

                    for (childDesc in childDescriptors) {

                        val childSerializerParent =
                            DetachedParent(config, childDesc, qName, false, outputKind, isDefaultNamespace = false)

                        map[childDesc.serialName] =
                            from(config, serializersModule, childSerializerParent, tagParent, canBeAttribute = false)


                    }
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    public val parentSerialName: String? =
        tagParent.descriptor?.serialDescriptor?.serialName ?: serialDescriptor.capturedKClass?.maybeSerialName

    @OptIn(WillBePrivate::class) // the type ParentInfo should become internal
    private val children by lazy {
        List(elementsCount) { index ->
            val canBeAttribute = index == 0
            val overrideOutputKind = if (canBeAttribute) OutputKind.Attribute else OutputKind.Element
            val parent = ParentInfo(config, this, index, useOutputKind = overrideOutputKind)

            from(config = config, serializersModule = serializersModule, parent, canBeAttribute = canBeAttribute)
        }
    }


    override fun getElementDescriptor(index: Int): XmlDescriptor {
        return children[index]
    }

    public fun getPolymorphicDescriptor(typeName: String): XmlDescriptor {
        return polyInfo[typeName]
            ?: throw XmlSerialException("Missing polymorphic information for $typeName")
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
        if (parentSerialName != other.parentSerialName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + outputKind.hashCode()
        result = 31 * result + polymorphicMode.hashCode()
        result = 31 * result + polyInfo.hashCode()
        result = 31 * result + (parentSerialName?.hashCode() ?: 0)
        return result
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
internal fun SerialDescriptor.getNameInfo(parentNamespace: Namespace?, annotation: XmlSerialName?): DeclaredNameInfo {
    val realSerialName = when {
        isNullable && serialName.endsWith('?') -> serialName.dropLast(1)
        else -> capturedKClass?.maybeSerialName ?: serialName
    }

    val qName = annotation?.toQName(realSerialName, parentNamespace) ?: (this as? XmlSerialDescriptor)?.serialQName
    return DeclaredNameInfo(realSerialName, qName, annotation?.namespace == UNSET_ANNOTATION_VALUE)
}

public sealed class XmlListLikeDescriptor(
    policy: XmlSerializationPolicy,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo = serializerParent
) : XmlDescriptor(policy, serializerParent, tagParent) {

    public open val isListEluded: Boolean = when {
        tagParent is DetachedParent && tagParent.isDocumentRoot -> false
        else -> policy.isListEluded(serializerParent, tagParent)
    }

    @ExperimentalSerializationApi
    final override val doInline: Boolean get() = false

    @ExperimentalXmlUtilApi
    final override val preserveSpace: Boolean get() = false

    @OptIn(ExperimentalSerializationApi::class, ExperimentalXmlUtilApi::class)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlListLikeDescriptor

        if (isListEluded != other.isListEluded) return false
        if (doInline != other.doInline) return false
        return preserveSpace == other.preserveSpace
    }

    @OptIn(ExperimentalXmlUtilApi::class, ExperimentalSerializationApi::class)
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + isListEluded.hashCode()
        result = 31 * result + doInline.hashCode()
        result = 31 * result + preserveSpace.hashCode()
        return result
    }

}

public class XmlMapDescriptor internal constructor(
    config: XmlConfig,
    serializersModule: SerializersModule,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo = serializerParent,
) : XmlListLikeDescriptor(config.policy, serializerParent, tagParent) {

    override val outputKind: OutputKind get() = OutputKind.Element

    override val isIdAttr: Boolean get() = false

    public val isValueCollapsed: Boolean by lazy {
        config.policy.isMapValueCollapsed(serializerParent, valueDescriptor)
    }

    internal val entryName: QName by lazy {
        if (isValueCollapsed) {
            valueDescriptor.tagName
        } else {
            config.policy.mapEntryName(serializerParent, isListEluded)
        }
    }

    private val keyDescriptor: XmlDescriptor by lazy {
        val keyNameInfo = config.policy.mapKeyName(serializerParent)
        val parentInfo = ParentInfo(config, this, 0, keyNameInfo)
        val keyTagParent = InjectedParentTag(0, typeDescriptor[0], keyNameInfo, tagParent.namespace)
        from(config, serializersModule, parentInfo, keyTagParent, canBeAttribute = true)
    }

    private val valueDescriptor: XmlDescriptor by lazy {
        val valueNameInfo = config.policy.mapValueName(serializerParent, isListEluded)
        val parentInfo = ParentInfo(config, this, 1, valueNameInfo, OutputKind.Element)
        val valueTagParent = InjectedParentTag(0, typeDescriptor[1], valueNameInfo, tagParent.namespace)
        from(config, serializersModule, parentInfo, valueTagParent, canBeAttribute = true)
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


}

public class XmlListDescriptor internal constructor(
    config: XmlConfig,
    serializersModule: SerializersModule,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo = serializerParent,
) : XmlListLikeDescriptor(config.policy, serializerParent, tagParent) {

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
                val childDescriptor = serialDescriptor.getElementDescriptor(0)

                when (childDescriptor.kind) {
                    is PolymorphicKind -> when {
                        config.policy.isTransparentPolymorphic(
                            DetachedParent(config, childDescriptor, false),
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
                config.policy.attributeListDelimiters(
                    ParentInfo(config, this, 0, useNameInfo, outputKind),
                    tagParent
                )

            OutputKind.Text ->
                config.policy.textListDelimiters(
                    ParentInfo(config, this, 0, useNameInfo, outputKind),
                    tagParent
                )

            else -> emptyArray()
        }
    }

    private val childDescriptor: XmlDescriptor by lazy {
        val childrenNameAnnotation = tagParent.useAnnChildrenName

        val useNameInfo = when {
            childrenNameAnnotation != null -> DeclaredNameInfo(
                childrenNameAnnotation.value,
                childrenNameAnnotation.toQName(),
                childrenNameAnnotation.namespace == UNSET_ANNOTATION_VALUE
            )

            !isListEluded -> null // if we have a list, don't repeat the outer name (at least allow the policy to decide)

            else -> tagParent.elementUseNameInfo
        }

        from(config, serializersModule, ParentInfo(config, this, 0, useNameInfo, outputKind), tagParent, false)
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
        if (outputKind != other.outputKind) return false
        return childDescriptor == other.childDescriptor
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + isListEluded.hashCode()
        result = 31 * result + outputKind.hashCode()
        result = 31 * result + childDescriptor.hashCode()
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

    /** Value of the [XmlNamespaceDeclSpec] annotation */
    @ExperimentalXmlUtilApi
    public val useAnnNsDecls: List<Namespace>? get() = null

    /** The raw serial descriptor of the element*/
    public val elementSerialDescriptor: SerialDescriptor

    /** Overidden serializer of the element*/
    public val overriddenSerializer: KSerializer<*>?

    /** Type requirements derived from the use site */
    public val elementUseOutputKind: OutputKind?

    /** The namespace this element has */
    public val namespace: Namespace

    public fun copy(
        config: XmlConfig,
        useNameInfo: DeclaredNameInfo = elementUseNameInfo,
        useOutputKind: OutputKind? = elementUseOutputKind,
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

    override val descriptor: Nothing? = null

    override val elementUseAnnotations: Collection<Annotation>
        get() = emptyList()

    override val elementSerialDescriptor: SerialDescriptor
        get() = elementTypeDescriptor.serialDescriptor

    override fun copy(
        config: XmlConfig,
        useNameInfo: DeclaredNameInfo,
        useOutputKind: OutputKind?,
        overriddenSerializer: KSerializer<*>?
    ): InjectedParentTag {
        return InjectedParentTag(
            index,
            elementTypeDescriptor,
            useNameInfo,
            namespace,
            useOutputKind,
            overriddenSerializer
        )
    }
}

private class DetachedParent(
    override val elementTypeDescriptor: XmlTypeDescriptor,
    override val elementUseNameInfo: DeclaredNameInfo,
    val isDocumentRoot: Boolean,
    outputKind: OutputKind? = null,
    override val overriddenSerializer: KSerializer<*>? = null,
) : SafeParentInfo {

    constructor(
        config: XmlConfig,
        serialDescriptor: SerialDescriptor,
        elementUseNameInfo: DeclaredNameInfo,
        isDocumentRoot: Boolean,
        outputKind: OutputKind? = null,
        overriddenSerializer: KSerializer<*>? = null,
    ) : this(
        (overriddenSerializer?.descriptor?.getXmlOverride() ?: serialDescriptor).let { descriptor ->
            val namespace = elementUseNameInfo.annotatedName?.toNamespace() ?: DEFAULT_NAMESPACE
            config.formatCache.lookupType(namespace, descriptor) {
                XmlTypeDescriptor(config, descriptor, namespace)
            }
        },
        elementUseNameInfo, isDocumentRoot, outputKind, overriddenSerializer
    )

    constructor(
        config: XmlConfig,
        serialDescriptor: SerialDescriptor,
        useName: QName?,
        isDocumentRoot: Boolean,
        outputKind: OutputKind? = null,
        isDefaultNamespace: Boolean,
    ) : this(
        config.formatCache.lookupType(DEFAULT_NAMESPACE, serialDescriptor) {
            XmlTypeDescriptor(config, serialDescriptor, DEFAULT_NAMESPACE)
        },
        useName,
        isDocumentRoot,
        outputKind,
        isDefaultNamespace
    )

    @OptIn(ExperimentalSerializationApi::class)
    constructor(
        elementTypeDescriptor: XmlTypeDescriptor,
        useName: QName?,
        isDocumentRoot: Boolean,
        outputKind: OutputKind? = null,
        isDefaultNamespace: Boolean,
    ) : this(
        elementTypeDescriptor,
        DeclaredNameInfo(
            serialName = elementTypeDescriptor.serialDescriptor.run {
                capturedKClass?.maybeSerialName ?: getNameInfo(
                    DEFAULT_NAMESPACE,
                    elementTypeDescriptor.typeAnnXmlSerialName
                ).serialName
            },
            annotatedName = useName,
            isDefaultNamespace = isDefaultNamespace
        ),
        isDocumentRoot,
        outputKind
    )

    @OptIn(ExperimentalSerializationApi::class)
    constructor(
        config: XmlConfig,
        serialDescriptor: SerialDescriptor,
        isDocumentRoot: Boolean,
        outputKind: OutputKind? = null,
    ) : this(
        config.formatCache.lookupType(DEFAULT_NAMESPACE, serialDescriptor) {
            XmlTypeDescriptor(config, serialDescriptor, DEFAULT_NAMESPACE)
        },
        isDocumentRoot,
        outputKind
    )

    @OptIn(ExperimentalSerializationApi::class)
    constructor(
        elementTypeDescriptor: XmlTypeDescriptor,
        isDocumentRoot: Boolean,
        outputKind: OutputKind? = null,
    ) : this(
        elementTypeDescriptor,
        DeclaredNameInfo(
            serialName = elementTypeDescriptor.serialDescriptor.let { sd ->
                sd.capturedKClass?.maybeSerialName ?: sd.getNameInfo(DEFAULT_NAMESPACE, elementTypeDescriptor.typeAnnXmlSerialName).serialName
            },
            annotatedName = null,
            isDefaultNamespace = false
        ),
        isDocumentRoot,
        outputKind
    )

    override fun copy(
        config: XmlConfig,
        useNameInfo: DeclaredNameInfo,
        useOutputKind: OutputKind?,
        overriddenSerializer: KSerializer<*>?,
    ): DetachedParent {
        return DetachedParent(
            elementTypeDescriptor,
            useNameInfo,
            isDocumentRoot,
            useOutputKind,
            overriddenSerializer
        )
    }

    override val index: Int get() = -1

    override val descriptor: SafeXmlDescriptor? get() = null

    override val parentIsInline: Boolean get() = elementTypeDescriptor.serialDescriptor.isInline

    override val elementUseAnnotations: Collection<Annotation> get() = emptyList()

    override val elementSerialDescriptor get() = elementTypeDescriptor.serialDescriptor

    override val elementUseOutputKind: OutputKind? = outputKind

    override val namespace: Namespace
        get() = elementUseNameInfo.annotatedName?.toNamespace()
            ?: DEFAULT_NAMESPACE

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
        useNameInfo: DeclaredNameInfo,
        useOutputKind: OutputKind?,
        overriddenSerializer: KSerializer<*>?
    ): ParentInfo {
        return ParentInfo(config, descriptor, index, useNameInfo, useOutputKind, overriddenSerializer)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ParentInfo

        if (descriptor != other.descriptor) return false
        if (index != other.index) return false
        if (overriddenSerializer != other.overriddenSerializer) return false
        if (elementUseNameInfo != other.elementUseNameInfo) return false
        return elementUseOutputKind == other.elementUseOutputKind
    }

    override fun hashCode(): Int {
        var result = descriptor.hashCode()
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
    override val elementTypeDescriptor: XmlTypeDescriptor by lazy {
        when {
            overriddenSerializer != null -> {
                val elemDesc = overriddenSerializer.descriptor.getXmlOverride()
                config.formatCache.lookupType(descriptor.tagName, elemDesc) {
                    XmlTypeDescriptor(config, elemDesc, descriptor.tagName.toNamespace())
                }
            }

            index == -1 -> descriptor.typeDescriptor
            elementSerialDescriptor.kind == SerialKind.CONTEXTUAL ->
                descriptor.typeDescriptor

            else -> {
                val ns = descriptor.tagParent.namespace
                config.formatCache.lookupType(ns, elementSerialDescriptor) {
                    XmlTypeDescriptor(config, elementSerialDescriptor, ns)
                }
            }
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
        val annotations = descriptor.serialDescriptor.getElementAnnotations(index)
        for (an in annotations) {
            when (an) {
                is XmlSerialName -> useAnnXmlSerialName = an
                is XmlElement -> useAnnIsElement = an.value
                is XmlPolyChildren -> useAnnPolyChildren = an
                is XmlIgnoreWhitespace -> useAnnIgnoreWhitespace = an.value
                is XmlNamespaceDeclSpec -> useAnnNsDecls = an.namespaces
                is XmlChildrenName -> useAnnChildrenName = an
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

    @OptIn(ExperimentalSerializationApi::class)
    override val elementUseOutputKind: OutputKind? = useOutputKind ?: when (index) {
        -1 -> null
        else -> descriptor.serialDescriptor.getElementAnnotations(index).getRequestedOutputKind()
    }


}


private fun <T : Annotation> Iterable<T>.getRequestedOutputKind(): OutputKind? {
    var xmlCData: XmlCData? = null

    for (annotation in this) {
        when {
            (annotation as? XmlValue)?.value == true -> return OutputKind.Mixed
            annotation is XmlId ||
                    annotation is XmlOtherAttributes -> return OutputKind.Attribute

            annotation is XmlElement -> return if (annotation.value) OutputKind.Element else OutputKind.Attribute
            annotation is XmlPolyChildren || annotation is XmlChildrenName -> return OutputKind.Element
            annotation is XmlCData -> xmlCData = annotation
        }
    }
    if (xmlCData?.value == true) return OutputKind.Element

    return null
}


private fun polyTagName(
    config: XmlConfig,
    currentPkg: String,
    parentName: ActualNameInfo,
    polyChildSpecification: String,
    baseClass: KClass<*>,
    serializersModule: SerializersModule
): PolyBaseInfo {
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


    val elementTypeDescriptor = config.formatCache.lookupType(parentNamespace, descriptor) {
        XmlTypeDescriptor(config, descriptor, parentNamespace)
    }

    val name: QName = when {
        eqPos < 0 -> {
            descriptor.declRequestedName(parentNamespace, elementTypeDescriptor.typeAnnXmlSerialName)
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

private val List<Annotation>.hasXmlSerialDesriptorMarker: Boolean
    get() {
        if (size < 1) return false
        return get(0) is XmlSerialDescriptorMarker
    }
