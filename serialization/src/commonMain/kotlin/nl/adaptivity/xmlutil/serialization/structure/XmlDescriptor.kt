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

package nl.adaptivity.xmlutil.serialization.structure

import kotlinx.serialization.*
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.XmlCodecBase.Companion.declRequestedName
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.ActualNameInfo
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.DeclaredNameInfo
import nl.adaptivity.xmlutil.serialization.impl.ChildCollector
import nl.adaptivity.xmlutil.serialization.impl.capturedKClass
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.reflect.KClass

internal val SerialDescriptor.declDefault: String?
    get() = annotations.declDefault

internal val Collection<Annotation>.declDefault: String?
    get() = firstOrNull<XmlDefault>()?.value

internal fun SerialDescriptor.declOutputKind(): OutputKind? {
    for (a in annotations) {
        when (a) {
            is XmlValue -> return OutputKind.Text
            is XmlElement -> return if (a.value) OutputKind.Element else OutputKind.Attribute
            is XmlPolyChildren,
            is XmlChildrenName -> return OutputKind.Element
        }
    }
    return null
}

/**
 * Interface describing a type without providing access to child xml descriptors
 */
interface SafeXmlDescriptor {
    val isNullable: Boolean get() = serialDescriptor.isNullable
    val kind: SerialKind get() = serialDescriptor.kind
    val typeDescriptor: XmlTypeDescriptor
    val tagParent: SafeParentInfo
    val tagName: QName
    val serialDescriptor: SerialDescriptor
    val outputKind: OutputKind
    val elementsCount: Int
    val serialKind: SerialKind
    fun isElementOptional(index: Int): Boolean = serialDescriptor.isElementOptional(index)

}

sealed class XmlDescriptor(
    xmlCodecBase: XmlCodecBase,
    serializerParent: SafeParentInfo,
    final override val tagParent: SafeParentInfo = serializerParent,
    useNameInfo: DeclaredNameInfo = tagParent.elementUseNameInfo
                          ) : SafeXmlDescriptor {

    init {
        assert(useNameInfo == serializerParent.elementUseNameInfo)
    }

    protected val useNameInfo = serializerParent.elementUseNameInfo
    override val typeDescriptor: XmlTypeDescriptor = serializerParent.elemenTypeDescriptor

    override val tagName: QName by lazy {
        xmlCodecBase.config.policy.effectiveName(
            serializerParent,
            tagParent,
            outputKind,
            useNameInfo,
            serializerParent.elemenTypeDescriptor,
            typeDescriptor.serialDescriptor.kind,
            typeDescriptor.typeNameInfo
                                                )
    }

    override val serialDescriptor get() = typeDescriptor.serialDescriptor

    override val elementsCount: Int get() = typeDescriptor.serialDescriptor.elementsCount
    override val serialKind: SerialKind get() = typeDescriptor.serialDescriptor.kind

    open fun getElementDescriptor(index: Int): XmlDescriptor {
        throw IndexOutOfBoundsException("There are no children")
    }

    companion object {

        /**
         * @param serializerParent The descriptor for the directly preceding serializer
         * @param xmlCodecBase The codec base. This allows for some context dependend lookups such as prefixes
         * @param useNameInfo Name requirements based on name and annotations at the use site
         * @param tagParent Parent descriptor from the logical perspective (ignoring anomymous lists and polymorphic
         *                   subtags). In other words, the parent tag that is the kotlin object that "owns" the value (
         *                   and isn't builtin).
         * @param overrideOutputKind This can be passed to determine the effective outputkind. This is needed for things
         *                           like lists and polymorphic values.
         */
        internal fun from(
            serializerParent: SafeParentInfo,
            xmlCodecBase: XmlCodecBase,
            tagParent: SafeParentInfo = serializerParent
                         ): XmlDescriptor {
            val policy = xmlCodecBase.config.policy

            val effectiveOutputKind = run {
                policy.determineOutputKind(serializerParent, tagParent)
            }

            val useNameInfo: DeclaredNameInfo = serializerParent.elementUseNameInfo
            val overrideOutputKind: OutputKind? = serializerParent.elementUseOutputKind

            val useAnnotations = tagParent.elementUseAnnotations

            val useDefault: String? = useAnnotations.declDefault

            val serialDescriptor = serializerParent.elementSerialDescriptor

            val effectiveDefault = useDefault ?: serialDescriptor.declDefault
            val isValue = useAnnotations.firstOrNull<XmlValue>()?.value == true

            when (serialDescriptor.kind) {
                UnionKind.ENUM_KIND,
                is PrimitiveKind -> return XmlPrimitiveDescriptor(
                    serializerParent,
                    xmlCodecBase,
                    tagParent,
                    useNameInfo,
                    effectiveOutputKind,
                    effectiveDefault
                                                                 )
                StructureKind.LIST -> {
                    val isMixed = useAnnotations.firstOrNull<XmlValue>()?.value == true
                    val reqChildrenName = useAnnotations.firstOrNull<XmlChildrenName>()?.toQName()
                    val isListAnonymous = isMixed || reqChildrenName == null // TODO use the policy

                    return XmlListDescriptor(
                        xmlCodecBase,
                        serializerParent,
                        tagParent,
                        isListAnonymous,
                        useNameInfo
                                            )
                }
                //                StructureKind.MAP -> TODO("MAP")
                is PolymorphicKind -> {
                    val effectiveBaseclass = serialDescriptor.capturedKClass(xmlCodecBase.context)
                    val polyChildren = useAnnotations.firstOrNull<XmlPolyChildren>()
                    return XmlPolymorphicDescriptor(
                        serializerParent,
                        xmlCodecBase,
                        polyChildren,
                        tagParent,
                        effectiveBaseclass,
                        effectiveOutputKind
                                                   )
                }
                else               -> return XmlCompositeDescriptor(
                    serializerParent,
                    xmlCodecBase,
                    tagParent,
                    useNameInfo,
                    effectiveDefault
                                                                   )
            }
        }

        fun XmlSerializationPolicy.determineOutputKind(
            serializerParent: SafeParentInfo,
            tagParent: SafeParentInfo
                                                      ): OutputKind {
            val serialDescriptor = serializerParent.elementSerialDescriptor

            return when (val overrideOutputKind = serializerParent.elementUseOutputKind) {
                null -> {
                    val useAnnotations = tagParent.elementUseAnnotations
                    val isValue = useAnnotations.firstOrNull<XmlValue>()?.value == true
                    when {
                        isValue -> OutputKind.Mixed
                        else    -> tagParent.elementUseOutputKind ?: serialDescriptor.declOutputKind()
                        ?: defaultOutputKind(serialDescriptor.kind)
                    }
                }
                OutputKind.Mixed -> {
                    if (serializerParent.descriptor is XmlListDescriptor) {
                        OutputKind.Mixed
                    } else when (val outputKind =
                        (tagParent.elementUseOutputKind ?: serialDescriptor.declOutputKind()
                        ?: defaultOutputKind(
                            serialDescriptor.kind
                                            ))) {
                        OutputKind.Attribute -> OutputKind.Text
                        else                 -> outputKind
                    }
                }
                else             -> overrideOutputKind

            }
        }
    }
}

class XmlRootDescriptor
internal constructor(
    tagName: QName,
    descriptor: SerialDescriptor,
    private val xmlCodecBase: XmlCodecBase
                    ) :
    XmlDescriptor(xmlCodecBase, DetachedParent(descriptor, tagName)) {

    override val tagName: QName
        get() = useNameInfo.annotatedName!!

    override val outputKind: OutputKind get() = OutputKind.Mixed

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        if (index != 0) throw IndexOutOfBoundsException("There is exactly one child to a root tag")
        val parent = ParentInfo(this, -1)

        return from(tagParent, xmlCodecBase)
    }
}

sealed class XmlValueDescriptor(
    private val xmlCodecBase: XmlCodecBase,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo,
    useNameInfo: DeclaredNameInfo = tagParent.elementUseNameInfo,
    override val outputKind: OutputKind,
    val default: String? = null
                               ) :
    XmlDescriptor(xmlCodecBase, serializerParent, tagParent, useNameInfo) {

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
                    XmlDecoderBase(xmlCodecBase.context, xmlCodecBase.config, CompactFragment(default).getXmlReader())
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
    serializerParent: SafeParentInfo,
    xmlCodecBase: XmlCodecBase,
    tagParent: SafeParentInfo,
    useNameInfo: DeclaredNameInfo,
    outputKind: OutputKind,
    default: String? = null
                                                 ) :
    XmlValueDescriptor(xmlCodecBase, serializerParent, tagParent, useNameInfo, outputKind, default) {

}

class XmlCompositeDescriptor internal constructor(
    serializerParent: SafeParentInfo,
    xmlCodecBase: XmlCodecBase,
    tagParent: SafeParentInfo,
    useNameInfo: DeclaredNameInfo,
    default: String? = null
                                                 ) :
    XmlValueDescriptor(
        xmlCodecBase,
        serializerParent,
        tagParent,
        useNameInfo,
        outputKind = OutputKind.Element,
        default = default
                      ) {

    private val children: List<XmlDescriptor> by lazy {
        val valueChildIndex = getValueChild()

        List<XmlDescriptor>(elementsCount) { index ->
            from(ParentInfo(this, index), xmlCodecBase).also { desc ->
                if (valueChildIndex >= 0 && index != valueChildIndex && desc.outputKind == OutputKind.Element) {
                    throw XmlSerialException("Types with an @XmlValue member may not contain other child elements")
                }
            }
        }
    }

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        return children[index]
    }

    override fun toString(): String {
        return children.joinToString(",\n", "${tagName}: (\n", ")") { it.toString().prependIndent("    ") }
    }
}

class XmlPolymorphicDescriptor internal constructor(
    serializerParent: SafeParentInfo,
    xmlCodecBase: XmlCodecBase,
    xmlPolyChildren: XmlPolyChildren?,
    tagParent: SafeParentInfo,
    val baseClass: KClass<*>?,
    outputKind: OutputKind
                                                   ) :
    XmlValueDescriptor(
        xmlCodecBase,
        serializerParent,
        tagParent,
        outputKind = outputKind
                      ) {

    // xmlPolyChildren and sealed also leads to a transparent polymorphic
    val isTransparent = xmlCodecBase.config.autoPolymorphic || xmlPolyChildren != null

    val parentSerialName = tagParent.descriptor?.serialDescriptor?.serialName

    val polyInfo: Map<String, XmlDescriptor> = mutableMapOf<String, XmlDescriptor>().also { map ->

        val qName = when {
            isTransparent -> null
            else          -> QName("value")
        }

        when {
            xmlPolyChildren != null                         -> {
                val baseName =
                    ActualNameInfo(
                        tagParent.descriptor?.serialDescriptor?.serialName ?: "",
                        tagParent.descriptor?.tagName ?: QName("", "")
                                  )
                val baseClass = baseClass ?: Any::class
                for (polyChild in xmlPolyChildren.value) {
                    val childInfo = polyTagName(xmlCodecBase, baseName, polyChild, baseClass)

                    val childSerializerParent = DetachedParent(childInfo.descriptor, childInfo.tagName)

                    map[childInfo.describedName] = from(childSerializerParent, xmlCodecBase, tagParent)
                }
            }

            serialDescriptor.kind == PolymorphicKind.SEALED -> {
                // A sealed descriptor has 2 elements: 0 name: String, 1: value: elementDescriptor
                val d = serialDescriptor.getElementDescriptor(1)
                for (i in 0 until d.elementsCount) {
                    val childDesc = d.getElementDescriptor(i)
                    val typeName = childDesc.serialName

                    val childSerializerParent = DetachedParent(childDesc, qName)

                    map[typeName] = from(childSerializerParent, xmlCodecBase, tagParent)

                }
            }

            else                                            -> {
                val baseClass = when {
                    baseClass != null -> baseClass
                    else              -> serialDescriptor.capturedKClass(xmlCodecBase.context) ?: Any::class
                }

                val childCollector = ChildCollector(baseClass)
                xmlCodecBase.context.dumpTo(childCollector)

                for (child in childCollector.children) {
                    val childDesc = child.descriptor
                    val typeName = childDesc.serialName

                    val childSerializerParent = DetachedParent(childDesc, qName, outputKind)

                    map[typeName] = from(childSerializerParent, xmlCodecBase, tagParent)


                }
            }


        }
    }

    private val children by run {
        lazy {
            List<XmlDescriptor>(elementsCount) { index ->
                val overrideOutputKind = if (index == 0) OutputKind.Attribute else OutputKind.Element
                val parent = ParentInfo(this, index, useOutputKind = overrideOutputKind)

                from(parent, xmlCodecBase)
            }
        }
    }

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        return children[index]
    }

    fun getPolymorphicDescriptor(typeName: String): XmlDescriptor {
        return polyInfo[typeName]
            ?: throw XmlSerialException("Missing polymorphic information for $typeName")
    }
}

internal fun SerialDescriptor.getElementNameInfo(index: Int): DeclaredNameInfo {
    val serialName = getElementName(index)
    val qName = getElementAnnotations(index).firstOrNull<XmlSerialName>()?.toQName()
    return DeclaredNameInfo(serialName, qName)
}

internal fun SerialDescriptor.getNameInfo(): DeclaredNameInfo {
    val realSerialName = when {
        isNullable && serialName.endsWith('?') -> serialName.dropLast(1)
        else                                   -> serialName
    }
    val qName = annotations.firstOrNull<XmlSerialName>()?.toQName()
    return DeclaredNameInfo(realSerialName, qName)
}

class XmlListDescriptor internal constructor(
    private val xmlCodecBase: XmlCodecBase,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo = serializerParent,
    val isAnonymous: Boolean,
    useNameInfo: DeclaredNameInfo = serializerParent.elementUseNameInfo
                                            ) :
    XmlDescriptor(xmlCodecBase, serializerParent, tagParent, useNameInfo) {


    override val outputKind: OutputKind =
        if (tagParent.elementUseAnnotations
                .firstOrNull<XmlValue>() != null && xmlCodecBase.config.autoPolymorphic
        ) OutputKind.Mixed else OutputKind.Element

    private val childDescriptor: XmlDescriptor by lazy {
        val childrenName = tagParent.elementUseAnnotations.firstOrNull<XmlChildrenName>()?.toQName()
        val useNameInfo = when {
            childrenName != null -> DeclaredNameInfo(childrenName.localPart, childrenName)
            else                 -> useNameInfo
        }


        from(ParentInfo(this, 0, useNameInfo, outputKind), xmlCodecBase, tagParent)
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
    outputKind: OutputKind? = null
                            ) : SafeParentInfo {

    constructor(serialDescriptor: SerialDescriptor, useName: QName?, outputKind: OutputKind? = null) :
            this(serialDescriptor, DeclaredNameInfo(serialDescriptor.serialName, useName), outputKind)

    override fun copy(useNameInfo: DeclaredNameInfo, useOutputKind: OutputKind?): DetachedParent {
        return DetachedParent(serialDescriptor, useNameInfo, useOutputKind)
    }

    override val index: Int get() = -1

    override val descriptor: SafeXmlDescriptor? get() = null

    override val elemenTypeDescriptor get() = XmlTypeDescriptor(serialDescriptor)

    override val elementUseAnnotations: Collection<Annotation> get() = emptyList()

    override val elementSerialDescriptor get() = serialDescriptor

    override val elementUseOutputKind: OutputKind? = outputKind

    override val namespace: Namespace
        get() = elementUseNameInfo.annotatedName?.toNamespace() ?: XmlEvent.NamespaceImpl("", "")
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

    override val namespace: Namespace
        get() = descriptor.tagName.toNamespace()

    override val elemenTypeDescriptor: XmlTypeDescriptor
        get() = when (index) {
            -1 -> descriptor.typeDescriptor
            else -> XmlTypeDescriptor(elementSerialDescriptor)
        }

    override val elementUseNameInfo: DeclaredNameInfo = useNameInfo ?: when (index) {
        -1 -> DeclaredNameInfo(descriptor.serialDescriptor.serialName, null)
        else -> descriptor.serialDescriptor.getElementNameInfo(index)
    }

    override val elementUseAnnotations: Collection<Annotation>
        get() = when (index) {
            -1 -> emptyList()
            else -> descriptor.serialDescriptor.getElementAnnotations(index)
        }

    override val elementSerialDescriptor: SerialDescriptor
        get() {
            return when (index) {
                -1 -> descriptor.serialDescriptor
                else -> descriptor.serialDescriptor.getElementDescriptor(index)
            }
        }

    override val elementUseOutputKind: OutputKind? = useOutputKind ?: when (index) {
        -1 -> null
        else -> descriptor.serialDescriptor.getElementAnnotations(index).getRequestedOutputKind()
    }
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
    val descriptor = codecBase.context.getPolymorphic(baseClass, typename)?.descriptor
        ?: throw XmlException("Missing descriptor for $typename in the serial context")

    val name: QName = when {
        eqPos < 0 -> descriptor.declRequestedName(XmlEvent.NamespaceImpl(prefix, ns))
        else      -> QName(ns, localPart, prefix)
    }
    return PolyBaseInfo(name, descriptor)
}