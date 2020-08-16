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
import kotlinx.serialization.builtins.serializer
import nl.adaptivity.serialutil.impl.assert
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.NameInfo
import nl.adaptivity.xmlutil.serialization.canary.polyBaseClassName
import nl.adaptivity.xmlutil.serialization.impl.ChildCollector
import nl.adaptivity.xmlutil.serialization.impl.capturedKClass
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
        serialDescriptor: SerialDescriptor,
        xmlCodecBase: XmlCodecBase,
        useNameInfo: NameInfo
               ) : this(serialDescriptor, xmlCodecBase, useNameInfo)

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

    open val elementCount: Int get() = typeDescriptor.serialDescriptor.elementsCount
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
         * @param serializer The serializer for which a descriptor should be created
         * @param xmlCodecBase The codec base. This allows for some context dependend lookups such as prefixes
         * @param useName Name requirements based on name and annotations at the use site
         * @param tagParent Parent descriptor from the logical perspective (ignoring anomymous lists and polymorphic
         *                   subtags). In other words, the parent tag that is the kotlin object that "owns" the value (
         *                   and isn't builtin).
         * @param overrideOutputKind This can be passed to determine the effective outputkind. This is needed for things
         *                           like lists and polymorphic values.
         * @param useAnnotations The annotations at the usage/declaration that influence serialization
         */
        internal fun from(
            serializer: KSerializer<*>,
            serializerParent: ParentInfo,
            xmlCodecBase: XmlCodecBase,
            useName: NameInfo,
            tagParent: ParentInfo,
            overrideOutputKind: OutputKind? = null,
            useAnnotations: Collection<Annotation> = emptyList()
                         ): XmlDescriptor {
            return from(
                deserializer = serializer,
                serializerParent = serializerParent,
                xmlCodecBase = xmlCodecBase,
                useName = useName,
                tagParent = tagParent,
                overrideOutputKind = overrideOutputKind,
                useAnnotations = useAnnotations
                       )
        }

        /**
         * @param serializer The serializer for which a descriptor should be created
         * @param xmlCodecBase The codec base. This allows for some context dependend lookups such as prefixes
         * @param useName Name requirements based on name and annotations at the use site
         * @param tagParent Parent descriptor from the logical perspective (ignoring anomymous lists and polymorphic
         *                   subtags). In other words, the parent tag that is the kotlin object that "owns" the value (
         *                   and isn't builtin).
         * @param overrideOutputKind This can be passed to determine the effective outputkind. This is needed for things
         *                           like lists and polymorphic values.
         * @param useAnnotations The annotations at the usage/declaration that influence serialization
         */
        internal fun from(
            serializer: SerializationStrategy<*>,
            serializerParent: ParentInfo,
            xmlCodecBase: XmlCodecBase,
            useName: NameInfo,
            tagParent: ParentInfo,
            overrideOutputKind: OutputKind? = null,
            useAnnotations: Collection<Annotation> = emptyList()
                         ): XmlDescriptor {
            val baseClass = (serializer as? PolymorphicSerializer)?.baseClass

            return fromCommon(
                serializerParent,
                serializer.descriptor,
                xmlCodecBase,
                useName,
                tagParent,
                overrideOutputKind,
                useAnnotations
                             )
        }

        /**
         * @param deserializer The deserializer for which a descriptor should be created
         * @param xmlCodecBase The codec base. This allows for some context dependend lookups such as prefixes
         * @param useName Name requirements based on name and annotations at the use site
         * @param tagParent Parent descriptor from the logical perspective (ignoring anomymous lists and polymorphic
         *                   subtags). In other words, the parent tag that is the kotlin object that "owns" the value (
         *                   and isn't builtin).
         * @param overrideOutputKind This can be passed to determine the effective outputkind. This is needed for things
         *                           like lists and polymorphic values.
         * @param useAnnotations The annotations at the usage/declaration that influence serialization
         */
        internal fun from(
            deserializer: DeserializationStrategy<*>,
            serializerParent: ParentInfo,
            xmlCodecBase: XmlCodecBase,
            useName: NameInfo,
            tagParent: ParentInfo,
            overrideOutputKind: OutputKind? = null,
            useAnnotations: Collection<Annotation> = emptyList()
                         ): XmlDescriptor {
            val baseClass = (deserializer as? PolymorphicSerializer)?.baseClass

            return fromCommon(
                serializerParent,
                deserializer.descriptor,
                xmlCodecBase,
                useName,
                tagParent,
                overrideOutputKind,
                useAnnotations
                             )
        }

        internal fun fromCommon(
            serializerParent: ParentInfo,
            serialDescriptor: SerialDescriptor,
            xmlCodecBase: XmlCodecBase,
            useNameInfo: NameInfo,
            tagParent: ParentInfo,
            overrideOutputKind: OutputKind?,
            useAnnotations: Collection<Annotation>
                               ): XmlDescriptor {
            val policy = xmlCodecBase.config.policy
            val parentNamespace = tagParent.parentNamespace()

            val useDefault: String? = useAnnotations.firstOrNull<XmlDefault>()?.value

            val effectiveDefault = useDefault ?: serialDescriptor.declDefault()
            val isValue = useAnnotations.firstOrNull<XmlValue>()?.value == true
            val effectiveOutputKind = overrideOutputKind ?: if (!isValue) {
                serialDescriptor.declOutputKind() ?: policy.defaultOutputKind(serialDescriptor.kind)
            } else {
                OutputKind.Text
            }

            val name = policy.effectiveName(
                serialDescriptor.kind,
                effectiveOutputKind,
                parentNamespace,
                useNameInfo,
                serialDescriptor.getNameInfo()
                                           )

            when (serialDescriptor.kind) {
                UnionKind.ENUM_KIND,
                is PrimitiveKind   -> return XmlPrimitiveDescriptor(
                    serializerParent,
                    serialDescriptor,
                    xmlCodecBase,
                    useNameInfo,
                    tagParent,
                    if (effectiveOutputKind == OutputKind.Mixed) OutputKind.Text else effectiveOutputKind,
                    effectiveDefault
                                                                   )
                StructureKind.LIST -> {
                    val isMixed = useAnnotations.firstOrNull<XmlValue>()?.value == true
                    val reqChildrenName = useAnnotations.firstOrNull<XmlChildrenName>()?.toQName()
                    val isListAnonymous = isMixed || reqChildrenName == null // TODO use the policy

                    val childrenName =
                        reqChildrenName ?: when {
                            isListAnonymous -> useAnnotations.firstOrNull<XmlSerialName>()?.toQName()
                            else            -> null
                        }

                    return XmlListDescriptor(
                        serializerParent,
                        xmlCodecBase,
                        useNameInfo,
                        name,
                        childrenName,
                        isListAnonymous,
                        tagParent
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
                    serialDescriptor,
                    xmlCodecBase,
                    useNameInfo,
                    tagParent,
                    effectiveDefault
                                                                   )
            }
        }
    }
}

class XmlRootDescriptor
internal constructor(
    tagName: QName,
    descriptor: SerialDescriptor,
    xmlCodecBase: XmlCodecBase
                    ) :
    XmlDescriptor(descriptor, xmlCodecBase, NameInfo(descriptor.serialName, tagName)) {
    override val tagParent: Nothing
        get() = throw UnsupportedOperationException("Root tags have no parents")

    override val tagName: QName
        get() = useNameInfo.annotatedName!!

    override val outputKind: OutputKind get() = OutputKind.Mixed


}

sealed class XmlValueDescriptor(
    serializerParent: ParentInfo,
    serialDescriptor: SerialDescriptor,
    xmlCodecBase: XmlCodecBase,
    useNameInfo: NameInfo,
    override val tagParent: ParentInfo,
    override val outputKind: OutputKind,
    val default: String?
                               ) :
    XmlDescriptor(serializerParent, serialDescriptor, xmlCodecBase, useNameInfo)

class XmlPrimitiveDescriptor internal constructor(
    serializerParent: ParentInfo,
    serialDescriptor: SerialDescriptor,
    xmlCodecBase: XmlCodecBase,
    useNameInfo: NameInfo,
    tagParent: ParentInfo,
    outputKind: OutputKind,
    default: String? = null
                                                 ) :
    XmlValueDescriptor(serializerParent, serialDescriptor, xmlCodecBase, useNameInfo, tagParent, outputKind, default) {

    init {
        assert(outputKind != OutputKind.Mixed) { "It is not valid to have a value of mixed output type" }
    }
}

class XmlCompositeDescriptor internal constructor(
    serializerParent: ParentInfo,
    serialDescriptor: SerialDescriptor,
    private val xmlCodecBase: XmlCodecBase,
    useNameInfo: NameInfo,
    tagParent: ParentInfo,
    default: String? = null
                                                 ) :
    XmlValueDescriptor(
        serializerParent,
        serialDescriptor,
        xmlCodecBase,
        useNameInfo,
        tagParent,
        OutputKind.Element,
        default
                      ) {

    private val children = List<XmlUseDescriptor>(elementCount) { index ->
        val parentInfo = ParentInfo(this, index)
        XmlUseDescriptorImpl(
            parentInfo,
            index,
            xmlCodecBase,
            serialDescriptor.getElementAnnotations(index).getRequestedOutputKind(),
            declParent = parentInfo,
            useAnnotations = serialDescriptor.getElementAnnotations(index)
                            )
    }

    private val children2: List<XmlDescriptor> by lazy {
        List<XmlDescriptor>(elementCount) { index ->
            fromCommon(
                ParentInfo(this, index),
                serialDescriptor.getElementDescriptor(index),
                xmlCodecBase,
                serialDescriptor.getElementNameInfo(index),
                ParentInfo(this, index),
                serialDescriptor.getElementAnnotations(index).getRequestedOutputKind(),
                serialDescriptor.getElementAnnotations(index)
                      )
        }

    }

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        return children2[index]
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
        serializerParent.getElementSerialDescriptor(),
        xmlCodecBase,
        useNameInfo,
        tagParent,
        outputKind,
        null
                      ) {

    internal val declParent: ParentInfo get() = tagParent

    // xmlPolyChildren and sealed also leads to a transparent polymorphic
    val transparent = xmlCodecBase.config.autoPolymorphic || xmlPolyChildren != null

    val parentSerialName = tagParent.descriptor.serialDescriptor.serialName

    private val typeChildDescriptor = from(
        String.serializer(),
        serializerParent = ParentInfo(this, 0),
        xmlCodecBase = xmlCodecBase,
        useName = serialDescriptor.getElementNameInfo(0),
        tagParent = if (transparent) tagParent else ParentInfo(this, 0),
        overrideOutputKind = outputKind,
        useAnnotations = serialDescriptor.getElementAnnotations(0)
                                          )

    private val polyInfo2: Map<String, XmlDescriptor> = mutableMapOf<String, XmlDescriptor>().also { map ->

        val qName = when {
            transparent -> null
            else        -> QName("value")
        }

        val parentInfo = ParentInfo(this, 1)
        when {
            xmlPolyChildren != null                         -> {
                val baseName = NameInfo(tagParent.descriptor.serialDescriptor.serialName, tagParent.descriptor.tagName)
                val baseClass = baseClass ?: Any::class
                for (polyChild in xmlPolyChildren.value) {
                    val childInfo = xmlCodecBase.polyTagName(baseName, polyChild, -1, baseClass)
                    val typeName = childInfo.describedName

                    map[typeName] = fromCommon(
                        parentInfo,
                        childInfo.descriptor,
                        xmlCodecBase,
                        NameInfo(childInfo.describedName, childInfo.tagName),
                        tagParent,
                        outputKind,
                        emptyList()
                                              )
                }
            }
            serialDescriptor.kind == PolymorphicKind.SEALED -> {
                // A sealed descriptor has 2 elements: 0 name: String, 1: value: elementDescriptor
                val d = serialDescriptor.getElementDescriptor(1)
                for (i in 0 until d.elementsCount) {
                    val childDesc = d.getElementDescriptor(i)
                    val typeName = childDesc.serialName

                    map[typeName] = fromCommon(
                        parentInfo,
                        childDesc,
                        xmlCodecBase,
                        NameInfo(typeName, qName),
                        tagParent,
                        outputKind,
                        emptyList()
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

                    map[typeName] = fromCommon(
                        parentInfo,
                        childDesc,
                        xmlCodecBase,
                        NameInfo(typeName, qName),
                        tagParent,
                        outputKind,
                        emptyList()
                                              )


                }
                childCollector.getPolyInfo(tagName)
            }


        }
    }

    internal val polyInfo: XmlNameMap = when {
        xmlPolyChildren != null
                                                        -> {
            val baseClass = baseClass ?: Any::class
            xmlCodecBase.polyInfo(
                NameInfo(tagParent.descriptor.serialDescriptor.serialName, tagParent.descriptor.tagName),
                xmlPolyChildren.value,
                baseClass
                                 )
        }
        serialDescriptor.kind == PolymorphicKind.SEALED -> {
            // A sealed descriptor has 2 elements: 0 name: String, 1: value: elementDescriptor
            val d = serialDescriptor.getElementDescriptor(1)
            XmlNameMap().apply {
                for (i in 0 until d.elementsCount) {
                    val childDesc = d.getElementDescriptor(i)
                    val childNameInfo = childDesc.getNameInfo()

                    val effectiveChildName = childNameInfo.annotatedName
                        ?: xmlCodecBase.config.policy.serialNameToQName(
                            childDesc.serialName,
                            tagParent.parentNamespace()
                                                                       )
                    registerClass(effectiveChildName, d.getElementDescriptor(i).serialName, true)
                }
            }
        }

        else                                            -> {
            val childCollector = when {

                baseClass == null -> serialDescriptor.polyBaseClassName?.let { ChildCollector(it) } ?: ChildCollector(
                    Any::class
                                                                                                                     )
                else              -> ChildCollector(baseClass)
            }
            xmlCodecBase.context.dumpTo(childCollector)
            childCollector.getPolyInfo(tagName)
        }
    }


    private val children = run {
        val overrideOutputKind: OutputKind? = when {
            transparent -> outputKind
            else        -> OutputKind.Element
        }
        List<XmlUseDescriptorImpl>(elementCount) { index ->
            XmlUseDescriptorImpl(
                ParentInfo(this, index), index, xmlCodecBase, overrideOutputKind,
                tagParent, serialDescriptor.getElementAnnotations(index)
                                )
        }
    }

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        return children[index].typeDescriptor
    }

    fun getPolymorphicDescriptor(typeName: String): XmlDescriptor {
        return polyInfo2[typeName]
            ?: throw XmlSerialException("Missing polymorphic information for $typeName")
    }

    override fun getChildDescriptor(index: Int, actualSerializer: SerializationStrategy<*>): XmlDescriptor {
        return getPolymorphicDescriptor(actualSerializer.descriptor.serialName)
    }

    override fun getChildDescriptor(index: Int, actualDeserializer: DeserializationStrategy<*>): XmlDescriptor {
        return getPolymorphicDescriptor(actualDeserializer.descriptor.serialName)
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
    useNameInfo: NameInfo,
    tagName: QName,
    val childrenName: QName?,
    val anonymous: Boolean,
    override val tagParent: ParentInfo
                                            ) :
    XmlDescriptor(serializerParent, serializerParent.getElementSerialDescriptor(), xmlCodecBase, useNameInfo) {
    override val tagName = tagName

    val useAnnotations get() = tagParent.getElementAnnotations()

    override val outputKind: OutputKind =
        if (useAnnotations.firstOrNull<XmlValue>() != null && xmlCodecBase.config.autoPolymorphic) OutputKind.Mixed else OutputKind.Element

    private val childDescriptor: XmlDescriptor by lazy {
        fromCommon(
            ParentInfo(this, 0),
            this.serialDescriptor.getElementDescriptor(0),
            xmlCodecBase,
            useNameInfo,
            tagParent,
            outputKind,
            useAnnotations
                  )
    }

    val declParent get() = tagParent


    // TODO: deprecated
    private val children = List<XmlUseDescriptorImpl>(elementCount) { index ->
        XmlUseDescriptorImpl(
            ParentInfo(this, index),
            index,
            xmlCodecBase,
            outputKind,
            tagParent,
            useAnnotations = useAnnotations
                            )
    }

    override fun getElementDescriptor(index: Int): XmlDescriptor {
        return children[index].typeDescriptor
    }

}

class ParentInfo(val descriptor: XmlDescriptor, val index: Int) {
    fun parentNamespace(): Namespace {
        return descriptor.tagName.toNamespace()
    }

    fun getElementAnnotations(): Collection<Annotation> {
        return descriptor.serialDescriptor.getElementAnnotations(index)
    }

    fun getElementSerialDescriptor(): SerialDescriptor {
        return descriptor.serialDescriptor.getElementDescriptor(this.index)
    }
}