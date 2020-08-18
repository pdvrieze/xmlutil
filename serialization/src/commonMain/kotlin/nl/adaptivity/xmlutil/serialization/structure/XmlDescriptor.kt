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
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.NameInfo
import nl.adaptivity.xmlutil.serialization.canary.polyBaseClassName
import nl.adaptivity.xmlutil.serialization.impl.ChildCollector
import nl.adaptivity.xmlutil.serialization.impl.capturedKClass
import nl.adaptivity.xmlutil.toNamespace
import kotlin.reflect.KClass

private fun SerialDescriptor.declDefault(): String? =
    annotations.firstOrNull<XmlDefault>()?.value

private fun SerialDescriptor.declOutputKind(): OutputKind? {
    for (a in annotations) {
        when (a) {
            is XmlValue        -> return OutputKind.Text
            is XmlElement      -> return if (a.value) OutputKind.Element else OutputKind.Attribute
            is XmlPolyChildren,
            is XmlChildrenName -> return OutputKind.Element
        }
    }
    return null
}

sealed class XmlDescriptor(
    serialDescriptor: SerialDescriptor,
    xmlCodecBase: XmlCodecBase,
    protected val useNameInfo: NameInfo
                          ) {

    constructor(
        serializerParent: ParentInfo,
        xmlCodecBase: XmlCodecBase,
        useNameInfo: NameInfo
               ) : this(serializerParent.getElementSerialDescriptor(), xmlCodecBase, useNameInfo)

    fun isElementOptional(index: Int): Boolean = serialDescriptor.isElementOptional(index)

    val isNullable: Boolean get() = serialDescriptor.isNullable

    val kind: SerialKind get() = serialDescriptor.kind

    val typeDescriptor: XmlTypeDescriptor = XmlTypeDescriptor(serialDescriptor, xmlCodecBase)

    abstract val tagParent: ParentInfo

    open val tagName: QName by lazy {
        xmlCodecBase.config.policy.effectiveName(
            serialDescriptor.kind,
            outputKind,
            tagParent.parentNamespace(),
            useNameInfo,
            typeDescriptor.typeNameInfo
                                                )
    }

    val serialDescriptor get() = typeDescriptor.serialDescriptor

    abstract val outputKind: OutputKind

    open val elementsCount: Int get() = typeDescriptor.serialDescriptor.elementsCount
    val serialKind: SerialKind get() = typeDescriptor.serialDescriptor.kind

    open fun getElementDescriptor(index: Int): XmlDescriptor {
        throw IndexOutOfBoundsException("There are no children")
    }

    open fun getChildDescriptor(index: Int, serializer: SerializationStrategy<*>): XmlDescriptor {
        return getElementDescriptor(index)
    }

    open fun getChildDescriptor(index: Int, serializer: KSerializer<*>): XmlDescriptor {
        return getElementDescriptor(index)
    }

    open fun getChildDescriptor(index: Int, deserializer: DeserializationStrategy<*>): XmlDescriptor {
        return getElementDescriptor(index)
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
            serializerParent: ParentInfo,
            xmlCodecBase: XmlCodecBase,
            tagParent: ParentInfo = serializerParent,
            useNameInfo: NameInfo = serializerParent.getElementNameInfo(),
            overrideOutputKind: OutputKind? = null
                         ): XmlDescriptor {
            val policy = xmlCodecBase.config.policy
            val parentNamespace = tagParent.parentNamespace()

            val useAnnotations = tagParent.getElementAnnotations()

            val useDefault: String? = useAnnotations.firstOrNull<XmlDefault>()?.value

            val serialDescriptor = serializerParent.getElementSerialDescriptor()

            val effectiveDefault = useDefault ?: serialDescriptor.declDefault()
            val isValue = useAnnotations.firstOrNull<XmlValue>()?.value == true
            val effectiveOutputKind = when (overrideOutputKind) {
                null             -> when {
                    isValue -> OutputKind.Mixed
                    else    -> tagParent.useOutputKind() ?: serialDescriptor.declOutputKind() ?: policy.defaultOutputKind(serialDescriptor.kind)
                }
                OutputKind.Mixed -> {
                    if (serializerParent.descriptor is XmlListDescriptor) {
                        OutputKind.Mixed
                    } else when (val outputKind = (tagParent.useOutputKind() ?: serialDescriptor.declOutputKind() ?: policy.defaultOutputKind(serialDescriptor.kind))) {
                        OutputKind.Attribute -> OutputKind.Text
                        else -> outputKind
                    }
                }
                else             -> overrideOutputKind

            }
            when (serialDescriptor.kind) {
                UnionKind.ENUM_KIND,
                is PrimitiveKind   -> return XmlPrimitiveDescriptor(
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
                        serializerParent,
                        xmlCodecBase,
                        isListAnonymous,
                        tagParent,
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
                        useNameInfo,
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
    }
}

class XmlPolymorphicParentDescriptor internal constructor(
    childDescriptor: SerialDescriptor,
    xmlCodecBase: XmlCodecBase,
    useNameInfo: NameInfo
                                                         ) :
    XmlDescriptor(childDescriptor, xmlCodecBase, useNameInfo) {
    override val tagParent: ParentInfo get() = ParentInfo(this, -1)
    override val outputKind: OutputKind get() = OutputKind.Element
}

class XmlRootDescriptor
internal constructor(
    tagName: QName,
    private val descriptor: SerialDescriptor,
    private val xmlCodecBase: XmlCodecBase
                    ) :
    XmlDescriptor(descriptor, xmlCodecBase, NameInfo(descriptor.serialName, tagName)) {
    override val tagParent: Nothing
        get() = throw UnsupportedOperationException("Root tags have no parents")

    override val tagName: QName
        get() = useNameInfo.annotatedName!!

    override val outputKind: OutputKind get() = OutputKind.Mixed

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        if (index != 0) throw IndexOutOfBoundsException("There is exactly one child to a root tag")
        val parent = ParentInfo(this, -1)

        return from(
            parent,
            xmlCodecBase,
            parent,
            NameInfo(descriptor.serialName, tagName)
                   )
    }
}

sealed class XmlValueDescriptor(
    serializerParent: ParentInfo,
    xmlCodecBase: XmlCodecBase,
    override val tagParent: ParentInfo,
    useNameInfo: NameInfo,
    override val outputKind: OutputKind,
    val default: String? = null
                               ) :
    XmlDescriptor(serializerParent, xmlCodecBase, useNameInfo)

class XmlPrimitiveDescriptor internal constructor(
    serializerParent: ParentInfo,
    xmlCodecBase: XmlCodecBase,
    tagParent: ParentInfo,
    useNameInfo: NameInfo,
    outputKind: OutputKind,
    default: String? = null
                                                 ) :
    XmlValueDescriptor(serializerParent, xmlCodecBase, tagParent, useNameInfo, outputKind, default) {

}

class XmlCompositeDescriptor internal constructor(
    serializerParent: ParentInfo,
    private val xmlCodecBase: XmlCodecBase,
    tagParent: ParentInfo,
    useNameInfo: NameInfo,
    default: String? = null
                                                 ) :
    XmlValueDescriptor(
        serializerParent,
        xmlCodecBase,
        tagParent,
        useNameInfo,
        OutputKind.Element,
        default
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
    serializerParent: ParentInfo,
    private val xmlCodecBase: XmlCodecBase,
    useNameInfo: NameInfo,
    xmlPolyChildren: XmlPolyChildren?,
    tagParent: ParentInfo,
    val baseClass: KClass<*>?,
    outputKind: OutputKind
                                                   ) :
    XmlValueDescriptor(
        serializerParent,
        xmlCodecBase,
        tagParent,
        useNameInfo,
        outputKind
                      ) {

    // xmlPolyChildren and sealed also leads to a transparent polymorphic
    val transparent = xmlCodecBase.config.autoPolymorphic || xmlPolyChildren != null

    val parentSerialName = tagParent.descriptor.serialDescriptor.serialName

    val polyInfo: Map<String, XmlDescriptor> = mutableMapOf<String, XmlDescriptor>().also { map ->

        val qName = when {
            transparent -> null
            else        -> QName("value")
        }
        val parentDesc = serializerParent.descriptor
        val inMixedParent = parentDesc.kind == StructureKind.LIST && parentDesc.outputKind == OutputKind.Mixed

        when {
            xmlPolyChildren != null                         -> {
                val baseName = NameInfo(tagParent.descriptor.serialDescriptor.serialName, tagParent.descriptor.tagName)
                val baseClass = baseClass ?: Any::class
                for (polyChild in xmlPolyChildren.value) {
                    val childInfo = xmlCodecBase.polyTagName(baseName, polyChild, -1, baseClass)
                    val typeName = childInfo.describedName
                    val polyParentDescriptor = XmlPolymorphicParentDescriptor(
                        childInfo.descriptor,
                        xmlCodecBase,
                        NameInfo(typeName, childInfo.tagName)
                                                                             )
                    val parentInfo = ParentInfo(polyParentDescriptor, -1)

                    map[typeName] = from(
                        parentInfo,
                        xmlCodecBase,
                        tagParent,
                        NameInfo(typeName, childInfo.tagName),
                        outputKind
                                        )
                }
            }

            serialDescriptor.kind == PolymorphicKind.SEALED -> {
                // A sealed descriptor has 2 elements: 0 name: String, 1: value: elementDescriptor
                val d = serialDescriptor.getElementDescriptor(1)
                for (i in 0 until d.elementsCount) {
                    val childDesc = d.getElementDescriptor(i)
                    val typeName = childDesc.serialName
                    val polyParentDescriptor =
                        XmlPolymorphicParentDescriptor(childDesc, xmlCodecBase, NameInfo(typeName, qName))
                    val parentInfo = ParentInfo(polyParentDescriptor, -1)

                    map[typeName] = from(
                        parentInfo,
                        xmlCodecBase,
                        tagParent,
                        NameInfo(typeName, qName),
                        outputKind
                                        )


                }
            }

            else                                            -> {
                val childCollector = when {

                    baseClass == null -> serialDescriptor.polyBaseClassName?.let { ChildCollector(it) }
                        ?: ChildCollector(Any::class)
                    else              -> ChildCollector(baseClass)
                }
                xmlCodecBase.context.dumpTo(childCollector)
                for (child in childCollector.children) {
                    val childDesc = child.descriptor
                    val typeName = childDesc.serialName
                    val polyParentDescriptor =
                        XmlPolymorphicParentDescriptor(childDesc, xmlCodecBase, NameInfo(typeName, qName))
                    val parentInfo = ParentInfo(polyParentDescriptor, -1)

                    map[typeName] = from(
                        parentInfo,
                        xmlCodecBase,
                        tagParent,
                        NameInfo(typeName, qName),
                        outputKind
                                        )


                }
                childCollector.getPolyInfo(tagName)
            }


        }
    }

    private val children by run {
        val overrideOutputKind: OutputKind? = when {
            transparent -> outputKind
            else        -> OutputKind.Element
        }
        lazy {
            List<XmlDescriptor>(elementsCount) { index ->
                from(
                    ParentInfo(this, index), xmlCodecBase, tagParent,
                    overrideOutputKind = if (index == 0) OutputKind.Attribute else OutputKind.Element // make this an element
                    )
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

    override fun getChildDescriptor(index: Int, serializer: SerializationStrategy<*>): XmlDescriptor {
        return getPolymorphicDescriptor(serializer.descriptor.serialName)
    }

    override fun getChildDescriptor(index: Int, deserializer: DeserializationStrategy<*>): XmlDescriptor {
        return getPolymorphicDescriptor(deserializer.descriptor.serialName)
    }
}

private fun SerialDescriptor.getElementDefault(index: Int): String? {
    return getElementAnnotations(index).firstOrNull<XmlDefault>()?.value
}

internal fun SerialDescriptor.getElementNameInfo(index: Int): NameInfo {
    val serialName = getElementName(index)
    val qName = getElementAnnotations(index).firstOrNull<XmlSerialName>()?.toQName()
    return NameInfo(serialName, qName)
}

internal fun SerialDescriptor.getNameInfo(): NameInfo {
    val realSerialName = when {
        isNullable && serialName.endsWith('?') -> serialName.dropLast(1)
        else                                   -> serialName
    }
    val qName = annotations.firstOrNull<XmlSerialName>()?.toQName()
    return NameInfo(realSerialName, qName)
}

class XmlListDescriptor internal constructor(
    serializerParent: ParentInfo,
    private val xmlCodecBase: XmlCodecBase,
    val isAnonymous: Boolean,
    override val tagParent: ParentInfo = serializerParent,
    useNameInfo: NameInfo = serializerParent.getElementNameInfo()
                                            ) :
    XmlDescriptor(serializerParent, xmlCodecBase, useNameInfo) {


    override val outputKind: OutputKind =
        if (tagParent.getElementAnnotations()
                .firstOrNull<XmlValue>() != null && xmlCodecBase.config.autoPolymorphic
        ) OutputKind.Mixed else OutputKind.Element

    private val childDescriptor: XmlDescriptor by lazy {
        val childrenName = tagParent.getElementAnnotations().firstOrNull<XmlChildrenName>()?.toQName()

        from(
            ParentInfo(this, 0),
            xmlCodecBase,
            tagParent,
            childrenName?.let { NameInfo(it.getLocalPart(), it) } ?: useNameInfo,
            outputKind // copy it here as parameter as it must be either a tag or mixed content.
            )
    }

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        return childDescriptor
    }

}

class ParentInfo(val descriptor: XmlDescriptor, val index: Int) {
    fun parentNamespace(): Namespace {
        return descriptor.tagName.toNamespace()
    }

    fun getElementNameInfo(): NameInfo {
        return when (index) {
            -1   -> NameInfo(descriptor.serialDescriptor.serialName, null)
            else -> descriptor.serialDescriptor.getElementNameInfo(index)
        }
    }

    fun getElementAnnotations(): Collection<Annotation> {
        return when (index) {
            -1   -> emptyList()
            else -> descriptor.serialDescriptor.getElementAnnotations(index)
        }
    }

    fun getElementSerialDescriptor(): SerialDescriptor {
        return when (index) {
            -1   -> descriptor.serialDescriptor
            else -> descriptor.serialDescriptor.getElementDescriptor(index)
        }
    }

    fun useOutputKind(): OutputKind? {
        return when (index) {
            -1 -> null
            else -> descriptor.serialDescriptor.getElementAnnotations(index).getRequestedOutputKind()
        }
    }
}