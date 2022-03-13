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

@file:Suppress("EXPERIMENTAL_API_USAGE")

package nl.adaptivity.xmlutil.serialization.structure

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.XmlCodecBase.Companion.declRequestedName
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.ActualNameInfo
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.DeclaredNameInfo
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.reflect.KClass

@ExperimentalXmlUtilApi
internal val SerialDescriptor.declDefault: String?
    get() = annotations.declDefault

internal val Collection<Annotation>.declDefault: String?
    get() = firstOrNull<XmlDefault>()?.value

@ExperimentalXmlUtilApi
internal fun SerialDescriptor.declOutputKind(): OutputKind? {
    for (a in annotations) {
        when {
            a is XmlValue && a.value -> return OutputKind.Text
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
public interface SafeXmlDescriptor {
    @ExperimentalSerializationApi
    public val isNullable: Boolean
        get() = serialDescriptor.isNullable

    @ExperimentalSerializationApi
    public val doInline: Boolean

    @ExperimentalSerializationApi
    public val kind: SerialKind
        get() = serialDescriptor.kind

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
    @Suppress("EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR") // it is actually private as sealed
    protected val xmlCodecBase: XmlCodecBase,
    serializerParent: SafeParentInfo,
    final override val tagParent: SafeParentInfo = serializerParent,
) : SafeXmlDescriptor {

    public val effectiveOutputKind: OutputKind
        get() = when (outputKind) {
            OutputKind.Inline -> getElementDescriptor(0).effectiveOutputKind
            else -> outputKind
        }

    public val policy: XmlSerializationPolicy get() = xmlCodecBase.config.policy

    final override val overriddenSerializer: KSerializer<*>? = serializerParent.overriddenSerializer

    protected val useNameInfo: DeclaredNameInfo = serializerParent.elementUseNameInfo

    override val typeDescriptor: XmlTypeDescriptor =
        serializerParent.elementTypeDescriptor

    public open val isUnsigned: Boolean get() = false

    override val tagName: QName by lazy {
        @OptIn(ExperimentalSerializationApi::class)
        policy.effectiveName(serializerParent, tagParent, outputKind, useNameInfo)
    }

    internal fun <V> effectiveSerializationStrategy(fallback: SerializationStrategy<V>): SerializationStrategy<V> {
        @Suppress("UNCHECKED_CAST")
        return (overriddenSerializer ?: fallback) as SerializationStrategy<V>
    }

    internal fun <V> effectiveDeserializationStrategy(fallback: DeserializationStrategy<V>): DeserializationStrategy<V> {
        @Suppress("UNCHECKED_CAST")
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

    internal companion object {

        /**
         * @param xmlCodecBase The codec base. This allows for some context dependend lookups such as prefixes
         * @param serializerParent The descriptor for the directly preceding serializer. This determines the actual
         *                           serialdescriptor.
         * @param tagParent Parent descriptor from the xml output perspective (ignoring anomymous lists and polymorphic
         *                   subtags). In other words, the parent tag that is the kotlin object that "owns" the value
         *                   (and isn't builtin). It is used to determine the applied annotations and requested tag
         *                   name.
         */
        @OptIn(ExperimentalSerializationApi::class)
        internal fun from(
            xmlCodecBase: XmlCodecBase,
            serializerParent: SafeParentInfo,
            tagParent: SafeParentInfo = serializerParent,
            canBeAttribute: Boolean,
        ): XmlDescriptor {
            val overridenSerializer = xmlCodecBase.config.policy.overrideSerializerOrNull(serializerParent, tagParent)

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
                    elementSerialDescriptor = overridenSerializer.descriptor
                    effectiveSerializerParent = serializerParent.copy(overriddenSerializer = overridenSerializer)
                    effectiveTagParent = tagParent.copy(overriddenSerializer = overridenSerializer)
                }
            }

            when (elementSerialDescriptor.kind) {
                SerialKind.ENUM,
                is PrimitiveKind ->
                    return XmlPrimitiveDescriptor(
                        xmlCodecBase,
                        effectiveSerializerParent,
                        effectiveTagParent,
                        canBeAttribute
                    )

                StructureKind.LIST ->
                    return XmlListDescriptor(xmlCodecBase, effectiveSerializerParent, effectiveTagParent)

                StructureKind.MAP -> {
                    if (serializerParent.elementUseOutputKind == OutputKind.Attribute) return XmlAttributeMapDescriptor(
                        xmlCodecBase,
                        effectiveSerializerParent,
                        effectiveTagParent
                    )
                }
                is PolymorphicKind ->
                    return XmlPolymorphicDescriptor(xmlCodecBase, effectiveSerializerParent, effectiveTagParent)

                else -> {} // fall through to other handler.
            }

            return when {
                xmlCodecBase.config.isInlineCollapsed &&
                        elementSerialDescriptor.isInline ->
                    XmlInlineDescriptor(xmlCodecBase, effectiveSerializerParent, effectiveTagParent, canBeAttribute)

                else ->
                    XmlCompositeDescriptor(xmlCodecBase, effectiveSerializerParent, effectiveTagParent)
            }
        }

    }
}

public class XmlRootDescriptor internal constructor(
    xmlCodecBase: XmlCodecBase,
    descriptor: SerialDescriptor,
    tagName: QName,
) : XmlDescriptor(xmlCodecBase, DetachedParent(descriptor, tagName, true, outputKind = null)) {

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = true // effectively a root descriptor is inline

    override val tagName: QName
        get() {
            val useNameInfo = useNameInfo
            return useNameInfo.annotatedName!!
        }

    override val outputKind: OutputKind get() = OutputKind.Mixed

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        if (index != 0) throw IndexOutOfBoundsException("There is exactly one child to a root tag")

        return from(xmlCodecBase, tagParent, canBeAttribute = false)
    }

    override fun toString(): String {
        return "<root>(\n${getElementDescriptor(0).toString().prependIndent("    ")}\n)"
    }
}

public sealed class XmlValueDescriptor(
    xmlCodecBase: XmlCodecBase,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
) : XmlDescriptor(xmlCodecBase, serializerParent, tagParent) {

    public final override val isCData: Boolean = (serializerParent.elementUseAnnotations.firstOrNull<XmlCData>()
        ?: tagParent.elementUseAnnotations.firstOrNull<XmlCData>()
        ?: serializerParent.elementSerialDescriptor.annotations.firstOrNull<XmlCData>())
        ?.value == true


    @OptIn(ExperimentalSerializationApi::class)
    public val default: String? = tagParent.elementUseAnnotations.declDefault
        ?: serializerParent.elementSerialDescriptor.declDefault

    private var defaultValue: Any? = UNSET

    public fun <T> defaultValue(deserializer: DeserializationStrategy<T>): T {
        defaultValue.let { d ->
            @Suppress("UNCHECKED_CAST")
            if (d != UNSET) return d as T
        }
        val d = when (default) {
            null -> null
            else -> {
                val defaultDecoder =
                    XmlDecoderBase(
                        xmlCodecBase.serializersModule,
                        xmlCodecBase.config,
                        CompactFragment(default).getXmlReader()
                    ).XmlDecoder(this)
                deserializer.deserialize(defaultDecoder)
            }
        }
        defaultValue = d
        @Suppress("UNCHECKED_CAST")
        return d as T
    }

    private object UNSET
}

public class XmlPrimitiveDescriptor internal constructor(
    xmlCodecBase: XmlCodecBase,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
    canBeAttribute: Boolean,
) :
    XmlValueDescriptor(xmlCodecBase, serializerParent, tagParent) {

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = false

    override val outputKind: OutputKind =
        xmlCodecBase.config.policy.effectiveOutputKind(serializerParent, tagParent, canBeAttribute)

    @OptIn(ExperimentalSerializationApi::class)
    override fun toString(): String = "$tagName:$kind = $outputKind"
}

public class XmlInlineDescriptor internal constructor(
    xmlCodecBase: XmlCodecBase,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
    canBeAttribute: Boolean,
) : XmlValueDescriptor(xmlCodecBase, serializerParent, tagParent) {

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = true

    init {
        if (!serializerParent.elementSerialDescriptor.isInline) {
            throw AssertionError("InlineDescriptors are only valid for inline classes")
        }
    }

    override val outputKind: OutputKind get() = child.outputKind//OutputKind.Inline

    private val child: XmlDescriptor by lazy {

        val effectiveUseNameInfo: DeclaredNameInfo = when {
            useNameInfo.annotatedName != null -> useNameInfo

            typeDescriptor.typeNameInfo.annotatedName != null -> typeDescriptor.typeNameInfo

            else -> {
                // This is needed as this descriptor is not complete yet and would use this element's
                // unset name for the namespace.
                val serialName = typeDescriptor.serialDescriptor.getElementName(0)
                val qName = typeDescriptor.serialDescriptor.getElementAnnotations(0).firstOrNull<XmlSerialName>()
                    ?.toQName(serialName, tagParent.namespace)
                val childUseNameInfo = DeclaredNameInfo(serialName, qName)

                when {
                    childUseNameInfo.annotatedName != null -> childUseNameInfo

                    else -> useNameInfo
                }

            }
        }

        val useParentInfo = ParentInfo(this, 0, effectiveUseNameInfo)

        from(xmlCodecBase, useParentInfo, tagParent, canBeAttribute)
    }

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        if (index != 0) throw IllegalArgumentException("Inline classes only have one child")
        return child
    }

    override fun toString(): String {
        return "$tagName (\n${child.toString().prependIndent("    ")}\n)"
    }

    override val isUnsigned: Boolean =
        serialDescriptor in UNSIGNED_SERIALIZER_DESCRIPTORS

    private companion object {
        @OptIn(ExperimentalSerializationApi::class)
        val UNSIGNED_SERIALIZER_DESCRIPTORS: Array<SerialDescriptor> = arrayOf(
            UByte.serializer().descriptor,
            UShort.serializer().descriptor,
            UInt.serializer().descriptor,
            ULong.serializer().descriptor
        )
    }
}

public class XmlAttributeMapDescriptor internal constructor(
    xmlCodecBase: XmlCodecBase,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
) : XmlValueDescriptor(xmlCodecBase, serializerParent, tagParent) {
    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = false
    override val outputKind: OutputKind get() = OutputKind.Attribute

    public val keyDescriptor: XmlDescriptor by lazy {
        from(
            xmlCodecBase,
            ParentInfo(this, 0, useOutputKind = OutputKind.Text),
            tagParent, true
        )
    }
    public val valueDescriptor: XmlDescriptor by lazy {
        from(
            xmlCodecBase,
            ParentInfo(this, 1, useOutputKind = OutputKind.Text),
            tagParent, true
        )
    }

    override val elementsCount: Int get() = 2

    override fun getElementDescriptor(index: Int): XmlDescriptor = when (index % 2) {
        0 -> keyDescriptor
        else -> valueDescriptor
    }

}

public class XmlCompositeDescriptor internal constructor(
    xmlCodecBase: XmlCodecBase,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
) : XmlValueDescriptor(xmlCodecBase, serializerParent, tagParent) {

    init {
        val requestedOutputKind = policy.effectiveOutputKind(serializerParent, tagParent, false)
        if (requestedOutputKind != OutputKind.Element) {
            policy.invalidOutputKind("Class SerialKinds/composites can only have Element output kinds, not $requestedOutputKind")
        }
    }

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = false

    override val outputKind: OutputKind get() = OutputKind.Element
    private val initialChildReorderInfo: Collection<XmlOrderConstraint>? =
        xmlCodecBase.config.policy.initialChildReorderMap(serialDescriptor)

    private val children: List<XmlDescriptor> by lazy {

        val valueChildIndex = getValueChild()

        val l = when {
            initialChildReorderInfo != null -> getElementDescriptors(initialChildReorderInfo)
            else -> List(elementsCount) { index -> createElementDescriptor(xmlCodecBase, index, true) }
        }

        if (valueChildIndex>=0) {
            val valueChild = l[valueChildIndex]
            if (valueChild.serialKind != StructureKind.LIST || valueChild.getElementDescriptor(0).serialDescriptor != CompactFragmentSerializer.descriptor) {
                val invalidIdx = l.indices
                    .firstOrNull() { idx -> idx !=valueChildIndex && l[idx].outputKind == OutputKind.Element}
                if (invalidIdx != null) {
                    throw XmlSerialException("Types with an @XmlValue member may not contain other child elements (${serialDescriptor.getElementDescriptor(invalidIdx)}")
                }
            }
        }

        l
    }

    private fun getElementDescriptors(initialChildReorderInfo: Collection<XmlOrderConstraint>): List<XmlDescriptor> {
        val descriptors = arrayOfNulls<XmlDescriptor>(elementsCount)

        fun XmlOrderNode.ensureDescriptor(): XmlDescriptor {
            return descriptors[this.elementIdx] ?: let {
                val canBeAttribute =
                    if (predecessors.isEmpty()) true else predecessors.all { it.ensureDescriptor().outputKind == OutputKind.Attribute }

                createElementDescriptor(xmlCodecBase, elementIdx, canBeAttribute).also {
                    descriptors[elementIdx] = it
                }
            }
        }

        for (orderedSequence in initialChildReorderInfo.sequenceStarts(elementsCount)) {
            for (element in orderedSequence.flatten()) {
                element.ensureDescriptor()
            }
        }

        return descriptors.requireNoNulls().toList()

    }


    private fun createElementDescriptor(
        xmlCodecBase: XmlCodecBase,
        index: Int,
        canBeAttribute: Boolean
    ): XmlDescriptor {
        return from(xmlCodecBase, ParentInfo(this, index), canBeAttribute = canBeAttribute)
    }

    override fun getElementDescriptor(index: Int): XmlDescriptor =
        children[index]

    public val childReorderMap: IntArray? by lazy {
        initialChildReorderInfo?.let {
            val newList = it.sequenceStarts(elementsCount)

            newList.fullFlatten(serialDescriptor, children)
        }
    }

    override fun toString(): String {
        return children.joinToString(",\n", "$tagName (\n", "\n)") {
            it.toString().prependIndent("    ")
        }
    }
}

public sealed class PolymorphicMode {
    public object TRANSPARENT : PolymorphicMode()
    public object TAG : PolymorphicMode()
    public class ATTR(public val name: QName) : PolymorphicMode()
}

public class XmlPolymorphicDescriptor internal constructor(
    xmlCodecBase: XmlCodecBase,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
) : XmlValueDescriptor(xmlCodecBase, serializerParent, tagParent) {

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = false

    override val outputKind: OutputKind =
        xmlCodecBase.config.policy.effectiveOutputKind(serializerParent, tagParent, canBeAttribute = false)

    public val polymorphicMode: PolymorphicMode
    public val isTransparent: Boolean get() = polymorphicMode == PolymorphicMode.TRANSPARENT
    public val polyInfo: Map<String, XmlDescriptor>

    init {
        val xmlPolyChildren = tagParent.elementUseAnnotations.firstOrNull<XmlPolyChildren>()

        // xmlPolyChildren and sealed also leads to a transparent polymorphic
        val polyAttrName = xmlCodecBase.config.policy.polymorphicDiscriminatorName(serializerParent, tagParent)
        polymorphicMode = when {
            xmlCodecBase.config.policy.isTransparentPolymorphic(serializerParent, tagParent) ->
                PolymorphicMode.TRANSPARENT
            polyAttrName == null -> PolymorphicMode.TAG
            else -> PolymorphicMode.ATTR(polyAttrName)
        }

        @OptIn(ExperimentalSerializationApi::class)
        polyInfo = mutableMapOf<String, XmlDescriptor>().also { map ->

            val qName = when (polymorphicMode) {
                PolymorphicMode.TRANSPARENT -> null
                PolymorphicMode.TAG -> from(xmlCodecBase, ParentInfo(this, 1), canBeAttribute = false).tagName
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
                        val childInfo = polyTagName(xmlCodecBase, baseName, polyChild, baseClass)

                        val childSerializerParent = DetachedParent(childInfo.descriptor, childInfo.tagName, false)

                        map[childInfo.describedName] =
                            from(xmlCodecBase, childSerializerParent, tagParent, canBeAttribute = false)
                    }
                }

                serialDescriptor.kind == PolymorphicKind.SEALED -> {
                    // A sealed descriptor has 2 elements: 0 name: String, 1: value: elementDescriptor
                    val d = serialDescriptor.getElementDescriptor(1)
                    for (i in 0 until d.elementsCount) {
                        val childDesc = d.getElementDescriptor(i)
                        val childSerializerParent = DetachedParent(childDesc, qName, false)

                        map[childDesc.serialName] =
                            from(xmlCodecBase, childSerializerParent, tagParent, canBeAttribute = false)

                    }
                }

                else -> {

                    val childDescriptors = xmlCodecBase.serializersModule.getPolymorphicDescriptors(serialDescriptor)

                    for (childDesc in childDescriptors) {

                        val childSerializerParent = DetachedParent(childDesc, qName, false, outputKind)

                        map[childDesc.serialName] =
                            from(xmlCodecBase, childSerializerParent, tagParent, canBeAttribute = false)


                    }
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    public val parentSerialName: String? = tagParent.descriptor?.serialDescriptor?.serialName

    private val children by lazy {
        List(elementsCount) { index ->
            val canBeAttribute = index == 0
            val overrideOutputKind = if (canBeAttribute) OutputKind.Attribute else OutputKind.Element
            val parent = ParentInfo(this, index, useOutputKind = overrideOutputKind)

            from(xmlCodecBase, parent, canBeAttribute = canBeAttribute)
        }
    }


    override fun getElementDescriptor(index: Int): XmlDescriptor {
        return children[index]
    }

    public fun getPolymorphicDescriptor(typeName: String): XmlDescriptor {
        return polyInfo[typeName]
            ?: throw XmlSerialException("Missing polymorphic information for $typeName")
    }

    override fun toString(): String = when (isTransparent) {
        true -> polyInfo.values.joinToString("\n", "[\n", "\n]") { "  - $it".indentNonFirst("    ") }
        else -> "$tagName (\n${getElementDescriptor(0).toString().prependIndent("    ")}\n" +
                polyInfo.values.joinToString(
                    "\n",
                    "    ${getElementDescriptor(1).tagName}: <poly> [\n",
                    "\n    ]\n)"
                ) { "- $it".prependIndent("        ") }
    }
}

@ExperimentalSerializationApi
internal fun SerialDescriptor.getElementNameInfo(index: Int, parentNamespace: Namespace?): DeclaredNameInfo {
    val serialName = getElementName(index)
    val qName = getElementAnnotations(index).firstOrNull<XmlSerialName>()?.toQName(serialName, parentNamespace)
    return DeclaredNameInfo(serialName, qName)
}

@ExperimentalSerializationApi
internal fun SerialDescriptor.getNameInfo(parentNamespace: Namespace?): DeclaredNameInfo {
    val realSerialName = when {
        isNullable && serialName.endsWith('?') -> serialName.dropLast(1)
        else -> serialName
    }
    val qName = annotations.firstOrNull<XmlSerialName>()?.toQName(realSerialName, parentNamespace)
    return DeclaredNameInfo(realSerialName, qName)
}

public class XmlListDescriptor internal constructor(
    xmlCodecBase: XmlCodecBase,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo = serializerParent,
) : XmlDescriptor(xmlCodecBase, serializerParent, tagParent) {

    @ExperimentalSerializationApi
    override val doInline: Boolean
        get() = false

    public val isListEluded: Boolean = when {
        tagParent is DetachedParent && tagParent.isDocumentRoot -> false
        else -> xmlCodecBase.config.policy.isListEluded(serializerParent, tagParent)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override val outputKind: OutputKind = when {
        tagParent.elementUseAnnotations.firstOrNull<XmlElement>()?.value == false -> {
            OutputKind.Attribute
        }
        !isListEluded -> OutputKind.Element

        tagParent.elementUseAnnotations.firstOrNull<XmlValue>() != null &&
                xmlCodecBase.config.policy.isTransparentPolymorphic(
                    DetachedParent(serialDescriptor.getElementDescriptor(0), null, false),
                    tagParent
                )
        -> OutputKind.Mixed

        else -> OutputKind.Element
    }

    private val childDescriptor: XmlDescriptor by lazy {
        val childrenName = tagParent.elementUseAnnotations.firstOrNull<XmlChildrenName>()?.toQName()

        val useNameInfo = when {
            childrenName != null -> DeclaredNameInfo(childrenName.localPart, childrenName)

            !isListEluded -> null // if we have a list, don't repeat the outer name (at least allow the policy to decide)

            else -> tagParent.elementUseNameInfo
        }

        from(xmlCodecBase, ParentInfo(this, 0, useNameInfo, outputKind), tagParent, false)
    }

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        return childDescriptor
    }

    override fun toString(): String = when (isListEluded){
        true -> "${tagName}: EludedList<$childDescriptor>"
        false -> "${tagName}: ExplicitList<$childDescriptor>"
    }
}

/**
 * Interface that provides parent info that does provide actual access to the child. As such it is safe to
 * be used to determine properties of the child.
 */
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


private class DetachedParent(
    private val serialDescriptor: SerialDescriptor,
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
        outputKind: OutputKind? = null
    ) : this(
        serialDescriptor,
        DeclaredNameInfo(serialDescriptor.serialName, useName),
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

    override val index: Int get() = -1

    override val descriptor: SafeXmlDescriptor? get() = null

    override val parentIsInline: Boolean get() = serialDescriptor.isInline

    override val elementTypeDescriptor
        get() = XmlTypeDescriptor(overriddenSerializer?.descriptor ?: serialDescriptor, namespace)

    override val elementUseAnnotations: Collection<Annotation> get() = emptyList()

    override val elementSerialDescriptor get() = overriddenSerializer?.descriptor ?: serialDescriptor

    override val elementUseOutputKind: OutputKind? = outputKind

    override val namespace: Namespace
        get() = elementUseNameInfo.annotatedName?.toNamespace()
            ?: XmlEvent.NamespaceImpl("", "")
}

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

    override val parentIsInline: Boolean get() = descriptor is XmlInlineDescriptor

    override val namespace: Namespace
        get() = descriptor.tagName.toNamespace()

    override val elementTypeDescriptor: XmlTypeDescriptor
        get() = when {
            overriddenSerializer != null -> XmlTypeDescriptor(
                overriddenSerializer.descriptor,
                descriptor.tagName.toNamespace()
            )
            index == -1 -> descriptor.typeDescriptor
            else -> XmlTypeDescriptor(elementSerialDescriptor, descriptor.tagParent.namespace)
        }

    @OptIn(ExperimentalSerializationApi::class)
    override val elementUseNameInfo: DeclaredNameInfo = useNameInfo ?: when (index) {
        -1 -> DeclaredNameInfo(descriptor.serialDescriptor.serialName, null)
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
                overriddenSerializer != null -> overriddenSerializer.descriptor

                index == -1 -> descriptor.serialDescriptor

                else -> descriptor.serialDescriptor.getElementDescriptor(index)
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
    codecBase: XmlCodecBase,
    parentName: ActualNameInfo,
    polyChildSpecification: String,
    baseClass: KClass<*>
): PolyBaseInfo {
    val currentPkg = parentName.serialName.substringBeforeLast('.', "")
    val parentTag = parentName.annotatedName
    val eqPos = polyChildSpecification.indexOf('=')
    val pkgPos: Int
    val prefPos: Int
    val typeNameBase: String
    val prefix: String
    val localPart: String

    if (eqPos < 0) {
        typeNameBase = polyChildSpecification
        pkgPos = polyChildSpecification.lastIndexOf('.')
        prefPos = -1
        prefix = parentTag.prefix
        localPart = if (pkgPos < 0) polyChildSpecification else polyChildSpecification.substring(pkgPos + 1)
    } else {
        typeNameBase = polyChildSpecification.substring(0, eqPos).trim()
        pkgPos = polyChildSpecification.lastIndexOf('.', eqPos - 1)
        prefPos = polyChildSpecification.indexOf(':', eqPos + 1)

        if (prefPos < 0) {
            prefix = parentTag.prefix
            localPart = polyChildSpecification.substring(eqPos + 1).trim()
        } else {
            prefix = polyChildSpecification.substring(eqPos + 1, prefPos).trim()
            localPart = polyChildSpecification.substring(prefPos + 1).trim()
        }
    }

    val ns = if (prefPos >= 0) codecBase.namespaceContext.getNamespaceURI(prefix)
        ?: parentTag.namespaceURI else parentTag.namespaceURI

    val typename = when {
        pkgPos != 0 || currentPkg.isEmpty() -> typeNameBase

        else -> "$currentPkg.${typeNameBase.substring(1)}"
    }

    @OptIn(ExperimentalSerializationApi::class)
    val descriptor = codecBase.serializersModule.getPolymorphic(baseClass, typename)?.descriptor
        ?: throw XmlException("Missing descriptor for $typename in the serial context")

    val name: QName = when {
        eqPos < 0 -> descriptor.declRequestedName(XmlEvent.NamespaceImpl(prefix, ns))
        else -> QName(ns, localPart, prefix)
    }
    return PolyBaseInfo(name, descriptor)
}

internal fun String.indentNonFirst(indent: String) =
    lineSequence().mapIndexed { index, s ->
        when {
            index == 0 -> s
            s.isBlank() -> {
                when {
                    s.length < indent.length -> indent
                    else -> s
                }
            }
            else -> indent + s
        }
    }.joinToString("\n")
