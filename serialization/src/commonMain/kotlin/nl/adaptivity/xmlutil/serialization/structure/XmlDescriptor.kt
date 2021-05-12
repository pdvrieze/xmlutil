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
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.XmlCodecBase.Companion.declRequestedName
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.ActualNameInfo
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.DeclaredNameInfo
import nl.adaptivity.xmlutil.serialization.impl.ChildCollector
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.reflect.KClass

@ExperimentalSerializationApi
internal val SerialDescriptor.declDefault: String?
    get() = annotations.declDefault

internal val Collection<Annotation>.declDefault: String?
    get() = firstOrNull<XmlDefault>()?.value

@ExperimentalSerializationApi
internal fun SerialDescriptor.declOutputKind(): OutputKind? {
    for (a in annotations) {
        when {
            a is XmlValue && a.value     -> return OutputKind.Text
            a is XmlElement              -> return if (a.value) OutputKind.Element else OutputKind.Attribute
            a is XmlPolyChildren ||
                    a is XmlChildrenName -> return OutputKind.Element
        }
    }
    return null
}

/**
 * Interface describing a type without providing access to child xml descriptors
 */
interface SafeXmlDescriptor {
    @OptIn(ExperimentalSerializationApi::class)
    val isNullable: Boolean
        get() = serialDescriptor.isNullable

    @OptIn(ExperimentalSerializationApi::class)
    abstract val doInline: Boolean

    @ExperimentalSerializationApi
    val kind: SerialKind
        get() = serialDescriptor.kind
    val typeDescriptor: XmlTypeDescriptor
    val tagParent: SafeParentInfo
    val tagName: QName
    val serialDescriptor: SerialDescriptor
    val outputKind: OutputKind
    val elementsCount: Int

    @ExperimentalSerializationApi
    val serialKind: SerialKind

    @OptIn(ExperimentalSerializationApi::class)
    fun isElementOptional(index: Int): Boolean = serialDescriptor.isElementOptional(index)

}

sealed class XmlDescriptor(
    @Suppress("EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR") // it is actually private as sealed
    protected val xmlCodecBase: XmlCodecBase,
    serializerParent: SafeParentInfo,
    final override val tagParent: SafeParentInfo = serializerParent
                          ) : SafeXmlDescriptor {

    val policy: XmlSerializationPolicy get() = xmlCodecBase.config.policy

    protected val useNameInfo = serializerParent.elementUseNameInfo

    override val typeDescriptor: XmlTypeDescriptor = serializerParent.elemenTypeDescriptor

    open val isUnsigned: Boolean get() = false

    override val tagName: QName by lazy {
        @OptIn(ExperimentalSerializationApi::class)
        policy.effectiveName(serializerParent, tagParent, outputKind, useNameInfo)
    }

    override val serialDescriptor get() = typeDescriptor.serialDescriptor

    @OptIn(ExperimentalSerializationApi::class)
    override val elementsCount: Int
        get() = typeDescriptor.serialDescriptor.elementsCount

    @ExperimentalSerializationApi
    override val serialKind: SerialKind
        get() = typeDescriptor.serialDescriptor.kind

    open fun getElementDescriptor(index: Int): XmlDescriptor {
        throw IndexOutOfBoundsException("There are no children")
    }

    companion object {

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
            tagParent: SafeParentInfo = serializerParent
                         ): XmlDescriptor {


            return when (serializerParent.elementSerialDescriptor.kind) {
                SerialKind.ENUM,
                is PrimitiveKind   ->
                    XmlPrimitiveDescriptor(xmlCodecBase, serializerParent, tagParent)

                StructureKind.LIST ->
                    XmlListDescriptor(xmlCodecBase, serializerParent, tagParent)

                //                StructureKind.MAP -> TODO("MAP")
                is PolymorphicKind ->
                    XmlPolymorphicDescriptor(xmlCodecBase, serializerParent, tagParent)

                else               -> when {
                    xmlCodecBase.config.isInlineCollapsed &&
                            serializerParent.elementSerialDescriptor.isInline
                         -> XmlInlineDescriptor(xmlCodecBase, serializerParent, tagParent)

                    else ->
                        XmlCompositeDescriptor(xmlCodecBase, serializerParent, tagParent)
                }
            }
        }

    }
}

class XmlRootDescriptor
internal constructor(xmlCodecBase: XmlCodecBase, descriptor: SerialDescriptor, tagName: QName) :
    XmlDescriptor(xmlCodecBase, DetachedParent(descriptor, tagName, true, outputKind = null)) {

    override val doInline: Boolean get() = true // effectively a root descriptor is inline

    override val tagName: QName
        get() {
            val useNameInfo = useNameInfo
            return useNameInfo.annotatedName!!
        }

    override val outputKind: OutputKind get() = OutputKind.Mixed

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        if (index != 0) throw IndexOutOfBoundsException("There is exactly one child to a root tag")

        return from(xmlCodecBase, tagParent)
    }

    override fun toString(): String {
        return "<root>(\n${getElementDescriptor(0).toString().prependIndent("    ")}\n)"
    }
}

sealed class XmlValueDescriptor(
    xmlCodecBase: XmlCodecBase,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo
                               ) :
    XmlDescriptor(xmlCodecBase, serializerParent, tagParent) {

    @OptIn(ExperimentalSerializationApi::class)
    val default = tagParent.elementUseAnnotations.declDefault ?: serializerParent.elementSerialDescriptor.declDefault

    private var defaultValue: Any? = UNSET

    fun <T> defaultValue(deserializer: DeserializationStrategy<T>): T {
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
                                  )
                        .XmlDecoder(this)
                deserializer.deserialize(defaultDecoder)
            }
        }
        defaultValue = d
        @Suppress("UNCHECKED_CAST")
        return d as T
    }

    private object UNSET
}

class XmlPrimitiveDescriptor internal constructor(
    xmlCodecBase: XmlCodecBase,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo
                                                 ) :
    XmlValueDescriptor(xmlCodecBase, serializerParent, tagParent) {

    override val doInline: Boolean get() = false

    override val outputKind: OutputKind = xmlCodecBase.config.policy.effectiveOutputKind(serializerParent, tagParent)

    @OptIn(ExperimentalSerializationApi::class)
    override fun toString(): String = "$tagName:$kind = $outputKind"
}

class XmlInlineDescriptor internal constructor(
    xmlCodecBase: XmlCodecBase,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo
) : XmlValueDescriptor(xmlCodecBase, serializerParent, tagParent) {

    override val doInline: Boolean get() = true

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

            ParentInfo(this, 0).elementUseNameInfo.annotatedName != null ->
                ParentInfo(this, 0).elementUseNameInfo

            else -> useNameInfo
        }

        val useParentInfo = ParentInfo(this, 0, effectiveUseNameInfo)

        from(xmlCodecBase, useParentInfo, tagParent)
    }

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        if (index != 0) throw IllegalArgumentException("Inline classes only have one child")
        return child
    }

    override fun toString(): String {
        return "${tagName} (\n${child.toString().prependIndent("    ")}\n)"
    }

    override val isUnsigned: Boolean = serialDescriptor in UNSIGNED_SERIALIZER_DESCRIPTORS

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        val UNSIGNED_SERIALIZER_DESCRIPTORS = arrayOf(
            UByte.serializer().descriptor,
            UShort.serializer().descriptor,
            UInt.serializer().descriptor,
            ULong.serializer().descriptor
        )
    }


}

class XmlCompositeDescriptor internal constructor(
    xmlCodecBase: XmlCodecBase,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo
                                                 ) :
    XmlValueDescriptor(xmlCodecBase, serializerParent, tagParent) {

    init {
        val requestedOutputKind = policy.effectiveOutputKind(serializerParent, tagParent)
        if (requestedOutputKind != OutputKind.Element) {
            policy.invalidOutputKind("Class SerialKinds/composites can only have Element output kinds, not $requestedOutputKind")
        }
    }

    override val doInline: Boolean get() = false

    override val outputKind: OutputKind get() = OutputKind.Element
    private val initialChildReorderInfo: List<XmlOrderNode>? = xmlCodecBase.config.policy.initialChildReorderMap(serialDescriptor)?.filter { it.predecessors.isEmpty() }


    private val children: List<XmlDescriptor> by lazy {
        val valueChildIndex = getValueChild()

        List<XmlDescriptor>(elementsCount) { index ->
            from(xmlCodecBase, ParentInfo(this, index)).also { desc ->
                if (valueChildIndex >= 0 && index != valueChildIndex && desc.outputKind == OutputKind.Element) {
                    throw XmlSerialException("Types with an @XmlValue member may not contain other child elements")
                }
            }
        }
    }

    override fun getElementDescriptor(index: Int): XmlDescriptor = children[index]

    val childReorderMap: IntArray? by lazy {

        initialChildReorderInfo?.let{
            xmlCodecBase.config.policy.updateReorderMap(it, children)
                .filter { it.predecessors.isEmpty() }
                .flatten(serialDescriptor, children)
        }
    }

    override fun toString(): String {
        return children.joinToString(",\n", "${tagName} (\n", "\n)") { it.toString().prependIndent("    ") }
    }
}

class XmlPolymorphicDescriptor internal constructor(
    xmlCodecBase: XmlCodecBase,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo
                                                   ) :
    XmlValueDescriptor(xmlCodecBase, serializerParent, tagParent) {

    override val doInline: Boolean get() = false

    override val outputKind: OutputKind =
        xmlCodecBase.config.policy.effectiveOutputKind(serializerParent, tagParent)

    val isTransparent: Boolean
    val polyInfo: Map<String, XmlDescriptor>

    init {
        val xmlPolyChildren = tagParent.elementUseAnnotations.firstOrNull<XmlPolyChildren>()

        // xmlPolyChildren and sealed also leads to a transparent polymorphic
        isTransparent = xmlCodecBase.config.policy.isTransparentPolymorphic(serializerParent, tagParent)

        @OptIn(ExperimentalSerializationApi::class)
        polyInfo = mutableMapOf<String, XmlDescriptor>().also { map ->

            val qName = when {
                isTransparent -> null
                else          -> from(xmlCodecBase, ParentInfo(this, 1)).tagName
            }

            when {
                xmlPolyChildren != null                         -> {
                    val baseName =
                        ActualNameInfo(
                            tagParent.descriptor?.serialDescriptor?.serialName ?: "",
                            tagParent.descriptor?.tagName ?: QName("", "")
                                      )
                    val baseClass = serialDescriptor.capturedKClass ?: Any::class

                    for (polyChild in xmlPolyChildren.value) {
                        val childInfo = polyTagName(xmlCodecBase, baseName, polyChild, baseClass)

                        val childSerializerParent = DetachedParent(childInfo.descriptor, childInfo.tagName, false)

                        map[childInfo.describedName] = from(xmlCodecBase, childSerializerParent, tagParent)
                    }
                }

                serialDescriptor.kind == PolymorphicKind.SEALED -> {
                    // A sealed descriptor has 2 elements: 0 name: String, 1: value: elementDescriptor
                    val d = serialDescriptor.getElementDescriptor(1)
                    for (i in 0 until d.elementsCount) {
                        val childDesc = d.getElementDescriptor(i)
                        val childSerializerParent = DetachedParent(childDesc, qName, false)

                        map[childDesc.serialName] = from(xmlCodecBase, childSerializerParent, tagParent)

                    }
                }

                else                                            -> {
                    val baseClass = serialDescriptor.capturedKClass ?: Any::class

                    val childCollector = ChildCollector(baseClass)
                    xmlCodecBase.serializersModule.dumpTo(childCollector)

                    for (child in childCollector.children) {
                        val childDesc = child.descriptor

                        val childSerializerParent = DetachedParent(childDesc, qName, false, outputKind)

                        map[childDesc.serialName] = from(xmlCodecBase, childSerializerParent, tagParent)


                    }
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    val parentSerialName = tagParent.descriptor?.serialDescriptor?.serialName

    private val children by lazy {
        List<XmlDescriptor>(elementsCount) { index ->
            val overrideOutputKind = if (index == 0) OutputKind.Attribute else OutputKind.Element
            val parent = ParentInfo(this, index, useOutputKind = overrideOutputKind)

            from(xmlCodecBase, parent)
        }
    }


    override fun getElementDescriptor(index: Int): XmlDescriptor {
        return children[index]
    }

    fun getPolymorphicDescriptor(typeName: String): XmlDescriptor {
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
internal fun SerialDescriptor.getElementNameInfo(index: Int): DeclaredNameInfo {
    val serialName = getElementName(index)
    val qName = getElementAnnotations(index).firstOrNull<XmlSerialName>()?.toQName()
    return DeclaredNameInfo(serialName, qName)
}

@ExperimentalSerializationApi
internal fun SerialDescriptor.getNameInfo(): DeclaredNameInfo {
    val realSerialName = when {
        isNullable && serialName.endsWith('?') -> serialName.dropLast(1)
        else                                   -> serialName
    }
    val qName = annotations.firstOrNull<XmlSerialName>()?.toQName()
    return DeclaredNameInfo(realSerialName, qName)
}

class XmlListDescriptor internal constructor(
    xmlCodecBase: XmlCodecBase,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo = serializerParent
                                            ) :
    XmlDescriptor(xmlCodecBase, serializerParent, tagParent) {

    override val doInline: Boolean get() = false

    val isListEluded = when {
        tagParent is DetachedParent && tagParent.isDocumentRoot -> false
        else ->xmlCodecBase.config.policy.isListEluded(serializerParent, tagParent)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override val outputKind: OutputKind = when {
        !isListEluded -> OutputKind.Element

        tagParent.elementUseAnnotations.firstOrNull<XmlValue>() != null &&
                xmlCodecBase.config.policy.isTransparentPolymorphic(
                    DetachedParent(serialDescriptor.getElementDescriptor(0), null, false),
                    tagParent
                                                                   )
                      -> OutputKind.Mixed

        else          -> OutputKind.Element
    }

    private val childDescriptor: XmlDescriptor by lazy {
        val childrenName = tagParent.elementUseAnnotations.firstOrNull<XmlChildrenName>()?.toQName()

        val useNameInfo = when {
            childrenName != null -> DeclaredNameInfo(childrenName.localPart, childrenName)

            ! isListEluded -> null // if we have a list, don't repeat the outer name (at least allow the policy to decide)
            else                 -> tagParent.elementUseNameInfo
        }

        from(xmlCodecBase, ParentInfo(this, 0, useNameInfo, outputKind), tagParent)
    }

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        return childDescriptor
    }

}

/**
 * Interface that provides parent info that does provide actual access to the child. As such it is safe to
 * be used to determine properties of the child.
 */
interface SafeParentInfo {
    val parentIsInline: Boolean
    val index: Int
    val descriptor: SafeXmlDescriptor?
    val elemenTypeDescriptor: XmlTypeDescriptor
    val elementUseNameInfo: DeclaredNameInfo
    val elementUseAnnotations: Collection<Annotation>
    val elementSerialDescriptor: SerialDescriptor
    val elementUseOutputKind: OutputKind?
    val namespace: Namespace

    fun copy(
        useNameInfo: DeclaredNameInfo = elementUseNameInfo,
        useOutputKind: OutputKind? = elementUseOutputKind
            ): SafeParentInfo
}

private class DetachedParent(
    private val serialDescriptor: SerialDescriptor,
    override val elementUseNameInfo: DeclaredNameInfo,
    val isDocumentRoot: Boolean,
    outputKind: OutputKind? = null
                            ) : SafeParentInfo {

    @OptIn(ExperimentalSerializationApi::class)
    constructor(serialDescriptor: SerialDescriptor, useName: QName?,isDocumentRoot: Boolean, outputKind: OutputKind? = null) :
            this(serialDescriptor, DeclaredNameInfo(serialDescriptor.serialName, useName), isDocumentRoot, outputKind)

    override fun copy(useNameInfo: DeclaredNameInfo, useOutputKind: OutputKind?): DetachedParent {
        return DetachedParent(serialDescriptor, useNameInfo, isDocumentRoot, useOutputKind)
    }

    override val index: Int get() = -1

    override val descriptor: SafeXmlDescriptor? get() = null

    override val parentIsInline: Boolean get() = serialDescriptor.isInline

    override val elemenTypeDescriptor get() = XmlTypeDescriptor(serialDescriptor)

    override val elementUseAnnotations: Collection<Annotation> get() = emptyList()

    override val elementSerialDescriptor get() = serialDescriptor

    override val elementUseOutputKind: OutputKind? = outputKind

    override val namespace: Namespace
        get() = elementUseNameInfo.annotatedName?.toNamespace()
            ?: XmlEvent.NamespaceImpl("", "")
}

class ParentInfo(
    override val descriptor: XmlDescriptor,
    override val index: Int,
    useNameInfo: DeclaredNameInfo? = null,
    useOutputKind: OutputKind? = null
                ) : SafeParentInfo {

    override fun copy(useNameInfo: DeclaredNameInfo, useOutputKind: OutputKind?): ParentInfo {
        return ParentInfo(descriptor, index, useNameInfo, useOutputKind)
    }

    override val parentIsInline: Boolean get() = descriptor is XmlInlineDescriptor

    override val namespace: Namespace
        get() = descriptor.tagName.toNamespace()

    override val elemenTypeDescriptor: XmlTypeDescriptor
        get() = when (index) {
            -1   -> descriptor.typeDescriptor
            else -> XmlTypeDescriptor(elementSerialDescriptor)
        }

    @OptIn(ExperimentalSerializationApi::class)
    override val elementUseNameInfo: DeclaredNameInfo = useNameInfo ?: when (index) {
        -1   -> DeclaredNameInfo(descriptor.serialDescriptor.serialName, null)
        else -> descriptor.serialDescriptor.getElementNameInfo(index)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override val elementUseAnnotations: Collection<Annotation>
        get() = when (index) {
            -1   -> emptyList()
            else -> descriptor.serialDescriptor.getElementAnnotations(index)
        }

    @OptIn(ExperimentalSerializationApi::class)
    override val elementSerialDescriptor: SerialDescriptor
        get() {
            return when (index) {
                -1   -> descriptor.serialDescriptor
                else -> descriptor.serialDescriptor.getElementDescriptor(index)
            }
        }

    @OptIn(ExperimentalSerializationApi::class)
    override val elementUseOutputKind: OutputKind? = useOutputKind ?: when (index) {
        -1   -> null
        else -> descriptor.serialDescriptor.getElementAnnotations(index).getRequestedOutputKind()
    }
}


private fun <T : Annotation> Iterable<T>.getRequestedOutputKind(): OutputKind? {
    for (annotation in this) {
        when (annotation) {
            is XmlValue        -> return OutputKind.Mixed
            is XmlElement      -> return if (annotation.value) OutputKind.Element else OutputKind.Attribute
            is XmlPolyChildren,
            is XmlChildrenName -> return OutputKind.Element
        }
    }
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
        pkgPos != 0 || currentPkg.isEmpty()
             -> typeNameBase

        else -> "$currentPkg.${typeNameBase.substring(1)}"
    }

    @OptIn(ExperimentalSerializationApi::class)
    val descriptor = codecBase.serializersModule.getPolymorphic(baseClass, typename)?.descriptor
        ?: throw XmlException("Missing descriptor for $typename in the serial context")

    val name: QName = when {
        eqPos < 0 -> descriptor.declRequestedName(XmlEvent.NamespaceImpl(prefix, ns))
        else      -> QName(ns, localPart, prefix)
    }
    return PolyBaseInfo(name, descriptor)
}

internal fun String.indentNonFirst(indent: String) =
    lineSequence().mapIndexed { index, s ->
        when {
            index == 0  -> s
            s.isBlank() -> {
                when {
                    s.length < indent.length -> indent
                    else                     -> s
                }
            }
            else        -> indent + s
        }
    }.joinToString("\n")
