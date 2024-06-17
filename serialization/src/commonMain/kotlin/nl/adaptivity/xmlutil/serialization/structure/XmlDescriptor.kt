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
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.impl.multiplatform.MpJvmDefaultWithCompatibility
import nl.adaptivity.xmlutil.core.impl.multiplatform.maybeAnnotations
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
        if (overriddenSerializer == (fallback as? KSerializer<Any>)?.nullable) return fallback
        return (overriddenSerializer ?: fallback) as DeserializationStrategy<V>
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
                    effectiveSerializerParent = serializerParent.copy(overriddenSerializer = overridenSerializer)
                    effectiveTagParent = tagParent.copy(overriddenSerializer = overridenSerializer)
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
                        serializerParent.elementUseAnnotations.filterIsInstance<XmlOtherAttributes>().isNotEmpty() ->
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
) : XmlDescriptor(config.policy, DetachedParent(descriptor, tagName, true, outputKind = null)) {

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
    public final override val isCData: Boolean = (serializerParent.elementUseAnnotations.firstOrNull<XmlCData>()
        ?: tagParent.elementUseAnnotations.firstOrNull<XmlCData>()
        ?: serializerParent.elementSerialDescriptor.annotations.firstOrNull<XmlCData>())
        ?.value == true


    @OptIn(ExperimentalXmlUtilApi::class)
    public val default: String? = tagParent.elementUseAnnotations.declDefault
        ?: serializerParent.elementSerialDescriptor.declDefault

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

    override val isIdAttr: Boolean = serializerParent.elementUseAnnotations.any { it is XmlId }

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

    override val isIdAttr: Boolean = serializerParent.elementUseAnnotations.any {
        it is XmlId
    }

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

        val useParentInfo = ParentInfo(this, 0, effectiveUseNameInfo)

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
            ParentInfo(this, 0, useOutputKind = OutputKind.Text),
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
            ParentInfo(this, 1, useOutputKind = OutputKind.Text),
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
        val overriddenParentInfo = DetachedParent(descriptor, useNameInfo, false)

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
    public val valueChild: Int = serialDescriptor.getValueChild()

    @OptIn(ExperimentalSerializationApi::class)
    @ExperimentalXmlUtilApi
    public val attrMapChild: Int by lazy { //uses elementDescriptor, so needs to be lazy
        var fallbackIdx = if (config.policy.isStrictOtherAttributes) Int.MAX_VALUE else -1
        for (i in 0 until elementsCount) {
            if (getElementDescriptor(i) is XmlAttributeMapDescriptor) {
                if (serialDescriptor.getElementAnnotations(i).firstOrNull<XmlOtherAttributes>() != null) {
                    fallbackIdx = i
                    break
                }
                if (fallbackIdx < 0) fallbackIdx = i
            }
        }
        if (fallbackIdx == Int.MAX_VALUE) -1 else fallbackIdx // fallbacks for old behaviour.
    }

    override val outputKind: OutputKind get() = OutputKind.Element
    private val initialChildReorderInfo: Collection<XmlOrderConstraint>? =
        config.policy.initialChildReorderMap(serialDescriptor)

    @OptIn(ExperimentalSerializationApi::class)
    private val children: List<XmlDescriptor> by lazy {

        val valueChildIndex = getValueChild()

        val l = when {
            initialChildReorderInfo != null -> getElementDescriptors(config, serializersModule, initialChildReorderInfo)
            else -> List(elementsCount) { index -> createElementDescriptor(config, serializersModule, index, true) }
        }

        if (valueChildIndex >= 0) {
            val valueChild = l[valueChildIndex]
            if (valueChild.serialKind != StructureKind.LIST ||
                valueChild.getElementDescriptor(0).serialDescriptor.let {
                    @Suppress("DEPRECATION")
                    it != DeprecatedCompactFragmentSerializer.descriptor && it != CompactFragmentSerializer.descriptor
                }) {
                val invalidIdx = l.indices
                    .firstOrNull { idx -> idx != valueChildIndex && l[idx].outputKind == OutputKind.Element }
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

        l
    }

    private fun getElementDescriptors(
        config: XmlConfig,
        serializersModule: SerializersModule,
        initialChildReorderInfo: Collection<XmlOrderConstraint>
    ): List<XmlDescriptor> {
        val descriptors = arrayOfNulls<XmlDescriptor>(elementsCount)

        fun XmlOrderNode.ensureDescriptor(): XmlDescriptor {
            return descriptors[this.elementIdx] ?: let {
                val canBeAttribute =
                    if (predecessors.isEmpty()) true else predecessors.all { it.ensureDescriptor().outputKind == OutputKind.Attribute }

                createElementDescriptor(config, serializersModule, elementIdx, canBeAttribute).also {
                    descriptors[elementIdx] = it
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

        return descriptors.requireNoNulls().toList()

    }


    private fun createElementDescriptor(
        config: XmlConfig,
        serializersModule: SerializersModule,
        index: Int,
        canBeAttribute: Boolean
    ): XmlDescriptor {
        return from(config, serializersModule, ParentInfo(this, index), canBeAttribute = canBeAttribute)
    }

    override fun getElementDescriptor(index: Int): XmlDescriptor =
        children[index]

    private val childReorderInfo: Pair<OrderMatrix, IntArray>? by lazy {
        initialChildReorderInfo?.let {
            val newList = it.sequenceStarts(elementsCount)

            newList.fullFlatten(serialDescriptor, children)
        }
    }

    public val childReorderMap: IntArray? get() = childReorderInfo?.second

    public val childConstraints: OrderMatrix? get() = childReorderInfo?.first

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
        val xmlPolyChildren = tagParent.elementUseAnnotations.firstOrNull<XmlPolyChildren>()

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
                    serializersModule = serializersModule, ParentInfo(this, 1), canBeAttribute = false
                ).tagName

                is PolymorphicMode.ATTR -> tagName
            }

            when {
                xmlPolyChildren != null -> {
                    val baseName = ActualNameInfo(
                        tagParent.descriptor?.serialDescriptor?.serialName ?: "",
                        tagParent.descriptor?.tagName ?: QName("", "")
                    )
                    val baseClass = serialDescriptor.capturedKClass ?: Any::class

                    for (polyChild in xmlPolyChildren.value) {
                        val childInfo = polyTagName(baseName, polyChild, baseClass, serializersModule)

                        val childSerializerParent =
                            DetachedParent(childInfo.descriptor, childInfo.tagName, false, isDefaultNamespace = false)

                        map[childInfo.describedName] =
                            from(config, serializersModule, childSerializerParent, tagParent, canBeAttribute = false)
                    }
                }

                serialDescriptor.kind == PolymorphicKind.SEALED -> {
                    // A sealed descriptor has 2 elements: 0 name: String, 1: value: elementDescriptor
                    val d = serialDescriptor.getElementDescriptor(1)
                    for (i in 0 until d.elementsCount) {
                        val childDesc = d.getElementDescriptor(i)
                        val childSerializerParent = DetachedParent(childDesc, qName, false, isDefaultNamespace = false)

                        map[childDesc.serialName] =
                            from(config, serializersModule, childSerializerParent, tagParent, canBeAttribute = false)

                    }
                }

                else -> {

                    val childDescriptors = serializersModule.getPolymorphicDescriptors(serialDescriptor)

                    for (childDesc in childDescriptors) {

                        val childSerializerParent =
                            DetachedParent(childDesc, qName, false, outputKind, isDefaultNamespace = false)

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
            val parent = ParentInfo(this, index, useOutputKind = overrideOutputKind)

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

@ExperimentalSerializationApi
internal fun SerialDescriptor.getElementNameInfo(index: Int, parentNamespace: Namespace?): DeclaredNameInfo {
    val serialName = getElementName(index)
    val annotation = getElementAnnotations(index).firstOrNull<XmlSerialName>()
    val qName = annotation?.toQName(serialName, parentNamespace)
    return DeclaredNameInfo(serialName, qName, annotation?.namespace == UNSET_ANNOTATION_VALUE)
}

@ExperimentalSerializationApi
internal fun SerialDescriptor.getNameInfo(parentNamespace: Namespace?): DeclaredNameInfo {
    val realSerialName = when {
        isNullable && serialName.endsWith('?') -> serialName.dropLast(1)
        else -> capturedKClass?.maybeSerialName ?: serialName
    }
    val annotation = annotations.firstOrNull<XmlSerialName>()
        ?: capturedKClass?.maybeAnnotations?.firstOrNull<XmlSerialName>()

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
        val parentInfo = ParentInfo(this, 0, keyNameInfo)
        val keyTagParent = InjectedParentTag(0, typeDescriptor[0], keyNameInfo, tagParent.namespace)
        from(config, serializersModule, parentInfo, keyTagParent, canBeAttribute = true)
    }

    private val valueDescriptor: XmlDescriptor by lazy {
        val valueNameInfo = config.policy.mapValueName(serializerParent, isListEluded)
        val parentInfo = ParentInfo(this, 1, valueNameInfo, OutputKind.Element)
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
            tagParent.elementUseAnnotations.firstOrNull<XmlElement>()?.value == false ->
                OutputKind.Attribute

            tagParent.elementUseAnnotations.firstOrNull<XmlId>() != null -> OutputKind.Attribute

            !isListEluded -> OutputKind.Element

            tagParent.elementUseAnnotations.firstOrNull<XmlValue>()?.value == true -> {
                val childDescriptor = serialDescriptor.getElementDescriptor(0)

                when (childDescriptor.kind) {
                    is PolymorphicKind -> when {
                        config.policy.isTransparentPolymorphic(
                            DetachedParent(childDescriptor, false),
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
                    ParentInfo(this, 0, useNameInfo, outputKind),
                    tagParent
                )

            OutputKind.Text ->
                config.policy.textListDelimiters(
                    ParentInfo(this, 0, useNameInfo, outputKind),
                    tagParent
                )

            else -> emptyArray()
        }
    }

    private val childDescriptor: XmlDescriptor by lazy {
        val childrenNameAnnotation = tagParent.elementUseAnnotations.firstOrNull<XmlChildrenName>()

        val useNameInfo = when {
            childrenNameAnnotation != null -> DeclaredNameInfo(
                childrenNameAnnotation.value,
                childrenNameAnnotation.toQName(),
                childrenNameAnnotation.namespace == UNSET_ANNOTATION_VALUE
            )

            !isListEluded -> null // if we have a list, don't repeat the outer name (at least allow the policy to decide)

            else -> tagParent.elementUseNameInfo
        }

        from(config, serializersModule, ParentInfo(this, 0, useNameInfo, outputKind), tagParent, false)
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

    /** The raw serial descriptor of the element*/
    public val elementSerialDescriptor: SerialDescriptor

    /** Overidden serializer of the element*/
    public val overriddenSerializer: KSerializer<*>?

    /** Type requirements derived from the use site */
    public val elementUseOutputKind: OutputKind?

    /** The namespace this element has */
    public val namespace: Namespace

    public fun copy(
        useNameInfo: DeclaredNameInfo = elementUseNameInfo,
        useOutputKind: OutputKind? = elementUseOutputKind,
        overriddenSerializer: KSerializer<*>? = this.overriddenSerializer
    ): SafeParentInfo

    public fun maybeOverrideSerializer(overriddenSerializer: KSerializer<*>?): SafeParentInfo =
        when (overriddenSerializer) {
            null -> this
            else -> copy(overriddenSerializer = overriddenSerializer)
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
    serialDescriptor: SerialDescriptor,
    override val elementUseNameInfo: DeclaredNameInfo,
    val isDocumentRoot: Boolean,
    outputKind: OutputKind? = null,
    override val overriddenSerializer: KSerializer<*>? = null,
) : SafeParentInfo {

    @OptIn(ExperimentalSerializationApi::class)
    constructor(
        serialDescriptor: SerialDescriptor,
        useName: QName?,
        isDocumentRoot: Boolean,
        outputKind: OutputKind? = null,
        isDefaultNamespace: Boolean,
    ) : this(
        serialDescriptor,

        DeclaredNameInfo(
            serialName = serialDescriptor.run {
                capturedKClass?.maybeSerialName ?: serialDescriptor.getNameInfo(DEFAULT_NAMESPACE).serialName
            },
            annotatedName = useName,
            isDefaultNamespace = isDefaultNamespace
        ),
        isDocumentRoot,
        outputKind
    )

    @OptIn(ExperimentalSerializationApi::class)
    constructor(
        serialDescriptor: SerialDescriptor,
        isDocumentRoot: Boolean,
        outputKind: OutputKind? = null,
    ) : this(
        serialDescriptor,
        DeclaredNameInfo(
            serialName = serialDescriptor.run {
                capturedKClass?.maybeSerialName ?: serialDescriptor.getNameInfo(DEFAULT_NAMESPACE).serialName
            },
            annotatedName = null,
            isDefaultNamespace = false
        ),
        isDocumentRoot,
        outputKind
    )

    override fun copy(
        useNameInfo: DeclaredNameInfo,
        useOutputKind: OutputKind?,
        overriddenSerializer: KSerializer<*>?,
    ): DetachedParent {
        return DetachedParent(serialDescriptor, useNameInfo, isDocumentRoot, useOutputKind, overriddenSerializer)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DetachedParent

        if (serialDescriptor != other.serialDescriptor) return false
        if (elementUseNameInfo != other.elementUseNameInfo) return false
        if (isDocumentRoot != other.isDocumentRoot) return false
        if (overriddenSerializer != other.overriddenSerializer) return false
        if (elementUseOutputKind != other.elementUseOutputKind) return false

        return true
    }

    override fun hashCode(): Int {
        var result = serialDescriptor.hashCode()
        result = 31 * result + elementUseNameInfo.hashCode()
        result = 31 * result + isDocumentRoot.hashCode()
        result = 31 * result + (overriddenSerializer?.hashCode() ?: 0)
        result = 31 * result + (elementUseOutputKind?.hashCode() ?: 0)
        return result
    }

    private val serialDescriptor = serialDescriptor.getXmlOverride()

    override val index: Int get() = -1

    override val descriptor: SafeXmlDescriptor? get() = null

    override val parentIsInline: Boolean get() = serialDescriptor.isInline

    override val elementTypeDescriptor
        get() = XmlTypeDescriptor(overriddenSerializer?.descriptor?.getXmlOverride() ?: serialDescriptor, namespace)

    override val elementUseAnnotations: Collection<Annotation> get() = emptyList()

    override val elementSerialDescriptor get() = overriddenSerializer?.descriptor?.getXmlOverride() ?: serialDescriptor

    override val elementUseOutputKind: OutputKind? = outputKind

    override val namespace: Namespace
        get() = elementUseNameInfo.annotatedName?.toNamespace()
            ?: DEFAULT_NAMESPACE


}

internal val DEFAULT_NAMESPACE = XmlEvent.NamespaceImpl("", "")

@WillBePrivate // 2021-07-05 Should not have been public.
public class ParentInfo(
    override val descriptor: XmlDescriptor,
    override val index: Int,
    useNameInfo: DeclaredNameInfo? = null,
    useOutputKind: OutputKind? = null,
    override val overriddenSerializer: KSerializer<*>? = null
) : SafeParentInfo {

    override fun copy(
        useNameInfo: DeclaredNameInfo,
        useOutputKind: OutputKind?,
        overriddenSerializer: KSerializer<*>?
    ): ParentInfo {
        return ParentInfo(descriptor, index, useNameInfo, useOutputKind, overriddenSerializer)
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
    override val elementTypeDescriptor: XmlTypeDescriptor
        get() = when {
            overriddenSerializer != null -> XmlTypeDescriptor(
                overriddenSerializer.descriptor.getXmlOverride(),
                descriptor.tagName.toNamespace()
            )

            index == -1 -> descriptor.typeDescriptor
            elementSerialDescriptor.kind == SerialKind.CONTEXTUAL ->
                descriptor.typeDescriptor

            else -> XmlTypeDescriptor(elementSerialDescriptor, descriptor.tagParent.namespace)
        }

    @OptIn(ExperimentalSerializationApi::class)
    override val elementUseNameInfo: DeclaredNameInfo = useNameInfo ?: when (index) {
        -1 -> DeclaredNameInfo(descriptor.serialDescriptor.serialName)
        else -> descriptor.serialDescriptor.getElementNameInfo(index, descriptor.tagName.toNamespace())
    }

    @OptIn(ExperimentalSerializationApi::class)
    override val elementUseAnnotations: Collection<Annotation>
        get() = when (index) {
            -1 -> emptyList()
            else -> descriptor.serialDescriptor.getElementAnnotations(index)
        }

    @OptIn(ExperimentalSerializationApi::class)
    override val elementSerialDescriptor: SerialDescriptor
        get() {
            return when {
                overriddenSerializer != null -> overriddenSerializer.descriptor.getXmlOverride()

                descriptor.serialKind == SerialKind.CONTEXTUAL ->
                    descriptor.serialDescriptor

                index == -1 -> descriptor.serialDescriptor

                else -> descriptor.serialDescriptor.getElementDescriptor(index).getXmlOverride()
            }
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
    parentName: ActualNameInfo,
    polyChildSpecification: String,
    baseClass: KClass<*>,
    serializersModule: SerializersModule
): PolyBaseInfo {
    val currentPkg = parentName.serialName.substringBeforeLast('.', "")
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

    @OptIn(ExperimentalSerializationApi::class)
    val descriptor = serializersModule.getPolymorphic(baseClass, typename)?.descriptor
        ?: throw XmlException("Missing descriptor for $typename in the serial context")

    val name: QName = when {
        eqPos < 0 -> descriptor.declRequestedName(XmlEvent.NamespaceImpl(prefix, parentTag.namespaceURI))
        else -> QName(parentTag.namespaceURI, localPart, prefix)
    }
    return PolyBaseInfo(name, descriptor)
}

internal fun <A : Appendable> A.appendIndent(count: Int) = apply {
    for (i in 0 until count) {
        append(' ')
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.getXmlOverride() = when {
    this is XmlSerialDescriptor -> xmlDescriptor
    isNullable && annotations.any { it is XmlSerialDescriptorMarker } -> getElementDescriptor(-1).nullable
    else -> this
}
