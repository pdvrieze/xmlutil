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
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.NameInfo
import nl.adaptivity.xmlutil.serialization.canary.polyBaseClassName
import nl.adaptivity.xmlutil.serialization.impl.ChildCollector
import nl.adaptivity.xmlutil.toNamespace
import nl.adaptivity.xmlutil.toQname
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
    val typeDescriptor: XmlTypeDescriptor = XmlTypeDescriptor(serialDescriptor, xmlCodecBase)

    abstract val parentTagDescriptor: XmlDescriptor

    open val tagName: QName by lazy { xmlCodecBase.config.policy.effectiveName(serialDescriptor.kind, outputKind, parentTagDescriptor.tagName.toNamespace(), useNameInfo, typeDescriptor.typeNameInfo) }

    val serialDescriptor get() = typeDescriptor.serialDescriptor

    abstract fun asElement(tagName: QName = this.tagName): XmlDescriptor
    abstract val outputKind: OutputKind

    open val elementCount: Int get() = typeDescriptor.serialDescriptor.elementsCount
    val serialKind: SerialKind get() = typeDescriptor.serialDescriptor.kind

    open fun getElementDescriptor(index: Int): XmlUseDescriptor {
        throw IndexOutOfBoundsException("There are no children")
    }

    open fun <T> getChildDescriptor(index: Int, serializer: SerializationStrategy<T>): XmlDescriptor {
        return getElementDescriptor(index).typeDescriptor
    }

    open fun <T> getChildDescriptor(index: Int, serializer: KSerializer<T>): XmlDescriptor {
        return getChildDescriptor(index, deserializer = serializer)
        return getElementDescriptor(index).typeDescriptor
    }

    open fun <T> getChildDescriptor(index: Int, deserializer: DeserializationStrategy<T>): XmlDescriptor {
        return getElementDescriptor(index).typeDescriptor
        throw IndexOutOfBoundsException("There are no children")
    }

    companion object {
        /**
         * @param serializer The serializer for which a descriptor should be created
         * @param xmlCodecBase The codec base. This allows for some context dependend lookups such as prefixes
         * @param useName Name requirements based on name and annotations at the use site
         * @param declParent Parent descriptor from the logical perspective (ignoring anomymous lists and polymorphic
         *                   subtags). In other words, the parent tag that is the kotlin object that "owns" the value (
         *                   and isn't builtin).
         * @param overrideOutputKind This can be passed to determine the effective outputkind. This is needed for things
         *                           like lists and polymorphic values.
         * @param useAnnotations The annotations at the usage/declaration that influence serialization
         */
        internal fun from(
            serializer: KSerializer<*>,
            xmlCodecBase: XmlCodecBase,
            useName: NameInfo,
            declParent: XmlDescriptor,
            overrideOutputKind: OutputKind? = null,
            useAnnotations: Collection<Annotation> = emptyList()
                         ): XmlDescriptor {
            return from(
                deserializer = serializer,
                xmlCodecBase = xmlCodecBase,
                useName = useName,
                declParent = declParent,
                overrideOutputKind = overrideOutputKind,
                useAnnotations = useAnnotations
                       )
        }

        /**
         * @param serializer The serializer for which a descriptor should be created
         * @param xmlCodecBase The codec base. This allows for some context dependend lookups such as prefixes
         * @param useName Name requirements based on name and annotations at the use site
         * @param declParent Parent descriptor from the logical perspective (ignoring anomymous lists and polymorphic
         *                   subtags). In other words, the parent tag that is the kotlin object that "owns" the value (
         *                   and isn't builtin).
         * @param overrideOutputKind This can be passed to determine the effective outputkind. This is needed for things
         *                           like lists and polymorphic values.
         * @param useAnnotations The annotations at the usage/declaration that influence serialization
         */
        internal fun from(
            serializer: SerializationStrategy<*>,
            xmlCodecBase: XmlCodecBase,
            useName: NameInfo,
            declParent: XmlDescriptor,
            overrideOutputKind: OutputKind? = null,
            useAnnotations: Collection<Annotation> = emptyList()
                         ): XmlDescriptor {
            val baseClass = (serializer as? PolymorphicSerializer)?.baseClass

            return fromCommon(
                serializer.descriptor,
                xmlCodecBase,
                useName,
                declParent,
                overrideOutputKind,
                useAnnotations,
                baseClass
                             )
        }

        /**
         * @param deserializer The deserializer for which a descriptor should be created
         * @param xmlCodecBase The codec base. This allows for some context dependend lookups such as prefixes
         * @param useName Name requirements based on name and annotations at the use site
         * @param declParent Parent descriptor from the logical perspective (ignoring anomymous lists and polymorphic
         *                   subtags). In other words, the parent tag that is the kotlin object that "owns" the value (
         *                   and isn't builtin).
         * @param overrideOutputKind This can be passed to determine the effective outputkind. This is needed for things
         *                           like lists and polymorphic values.
         * @param useAnnotations The annotations at the usage/declaration that influence serialization
         */
        internal fun from(
            deserializer: DeserializationStrategy<*>,
            xmlCodecBase: XmlCodecBase,
            useName: NameInfo,
            declParent: XmlDescriptor,
            overrideOutputKind: OutputKind? = null,
            useAnnotations: Collection<Annotation> = emptyList()
                         ): XmlDescriptor {
            val baseClass = (deserializer as? PolymorphicSerializer)?.baseClass

            return fromCommon(
                deserializer.descriptor,
                xmlCodecBase,
                useName,
                declParent,
                overrideOutputKind,
                useAnnotations,
                baseClass
                             )
        }

        internal fun fromCommon(
            serialDescriptor: SerialDescriptor,
            xmlCodecBase: XmlCodecBase,
            useNameInfo: NameInfo,
            parentTagDescriptor: XmlDescriptor,
            overrideOutputKind: OutputKind?,
            useAnnotations: Collection<Annotation>,
            baseClass: KClass<*>?
                               ): XmlDescriptor {
            val policy = xmlCodecBase.config.policy
            val parentNamespace = parentTagDescriptor.tagName.toNamespace()

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
                    serialDescriptor,
                    xmlCodecBase,
                    useNameInfo,
                    parentTagDescriptor,
                    name,
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
                        serialDescriptor,
                        xmlCodecBase,
                        useNameInfo,
                        name,
                        useAnnotations,
                        childrenName,
                        isListAnonymous,
                        parentTagDescriptor
                                            )
                }
                //                StructureKind.MAP -> TODO("MAP")
                is PolymorphicKind -> {
                    val polyChildren = useAnnotations.firstOrNull<XmlPolyChildren>()
                    return XmlPolymorphicDescriptor(
                        serialDescriptor,
                        xmlCodecBase,
                        useNameInfo,
                        name,
                        polyChildren,
                        parentTagDescriptor,
                        baseClass,
                        effectiveOutputKind
                                                   )
                }
                else               -> return XmlCompositeDescriptor(
                    serialDescriptor,
                    xmlCodecBase,
                    useNameInfo,
                    parentTagDescriptor,
                    name,
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
    override val parentTagDescriptor: Nothing
        get() = throw UnsupportedOperationException("Root tags have no parents")

    override val tagName: QName
        get() = useNameInfo.annotatedName!!

    override val outputKind: OutputKind get() = OutputKind.Mixed

    override fun asElement(tagName: QName): XmlDescriptor =
        throw UnsupportedOperationException("A dummy parent is not an element")


}

sealed class XmlValueDescriptor(
    serialDescriptor: SerialDescriptor,
    xmlCodecBase: XmlCodecBase,
    useNameInfo: NameInfo,
    override val parentTagDescriptor: XmlDescriptor,
    override val outputKind: OutputKind,
    val default: String?
                               ) :
    XmlDescriptor(serialDescriptor, xmlCodecBase, useNameInfo)

class XmlPrimitiveDescriptor internal constructor(
    serialDescriptor: SerialDescriptor,
    private val xmlCodecBase: XmlCodecBase,
    useNameInfo: NameInfo,
    parentTagDescriptor: XmlDescriptor,
    override val tagName: QName,
    outputKind: OutputKind,
    default: String? = null
                                                 ) :
    XmlValueDescriptor(serialDescriptor, xmlCodecBase, useNameInfo, parentTagDescriptor, outputKind, default) {

    init {
        assert(outputKind != OutputKind.Mixed) { "It is not valid to have a value of mixed output type" }
    }

    override fun asElement(tagName: QName): XmlDescriptor =
        XmlPrimitiveDescriptor(serialDescriptor, xmlCodecBase, useNameInfo, parentTagDescriptor, tagName, OutputKind.Element, default)
}

class XmlCompositeDescriptor internal constructor(
    serialDescriptor: SerialDescriptor,
    private val xmlCodecBase: XmlCodecBase,
    useNameInfo: NameInfo,
    parentTagDescriptor: XmlDescriptor,
    override val tagName: QName,
    default: String? = null
                                                 ) :
    XmlValueDescriptor(serialDescriptor, xmlCodecBase, useNameInfo, parentTagDescriptor, OutputKind.Element, default) {

    private val children = List<XmlUseDescriptor>(elementCount) { index ->
        XmlUseDescriptorImpl(
            this,
            index,
            xmlCodecBase,
            serialDescriptor.getElementAnnotations(index).getRequestedOutputKind(),
            declParent = this,
            useAnnotations = serialDescriptor.getElementAnnotations(index)
                            )
    }

    override fun getElementDescriptor(index: Int): XmlUseDescriptor {
        return children[index]
    }

    override fun asElement(tagName: QName): XmlDescriptor = this

    override fun toString(): String {
        return children.joinToString(",\n", "${tagName}: (\n", ")") { it.toString().prependIndent("    ") }
    }
}

class XmlPolymorphicDescriptor internal constructor(
    serialDescriptor: SerialDescriptor,
    private val xmlCodecBase: XmlCodecBase,
    useNameInfo: NameInfo,
    override val tagName: QName,
    xmlPolyChildren: XmlPolyChildren?,
    parentTagDescriptor: XmlDescriptor,
    val baseClass: KClass<*>?,
    outputKind: OutputKind
                                                   ) :
    XmlValueDescriptor(serialDescriptor, xmlCodecBase, useNameInfo, parentTagDescriptor, outputKind, null) {

    internal val declParent: XmlDescriptor get() = parentTagDescriptor

    // xmlPolyChildren and sealed also leads to a transparent polymorphic
    val transparent = xmlCodecBase.config.autoPolymorphic || xmlPolyChildren != null

    val parentSerialName = parentTagDescriptor.serialDescriptor.serialName

    private val typeChildDescriptor = from(
        String.serializer(),
        xmlCodecBase,
        serialDescriptor.getElementNameInfo(0),
        parentTagDescriptor,
        outputKind,
        useAnnotations = serialDescriptor.getElementAnnotations(0)
                                          )

    private val polyInfo2: Map<String, XmlUseDescriptor> = mutableMapOf<String, XmlUseDescriptor>().also { map ->

        val qName = when {
            transparent -> null
            else        -> QName("value")
        }

        when {
            xmlPolyChildren != null                         -> {
                val baseName =
                    NameInfo(parentTagDescriptor.serialDescriptor.serialName, parentTagDescriptor.tagName)
                val baseClass = baseClass ?: Any::class
                for (polyChild in xmlPolyChildren.value) {
                    val tagName = xmlCodecBase.polyTagName(baseName, polyChild, -1, baseClass)
                    val typeName = tagName.describedName

                    map[typeName] = XmlPolymorphicUseDescriptorImpl(
                        this, tagName.describedName, tagName.tagName,
                        xmlCodecBase, tagName.descriptor,
                        parentTagDescriptor, outputKind
                                                                   )
                }
            }
            serialDescriptor.kind == PolymorphicKind.SEALED -> {
                // A sealed descriptor has 2 elements: 0 name: String, 1: value: elementDescriptor
                val d = serialDescriptor.getElementDescriptor(1)
                for (i in 0 until d.elementsCount) {
                    val childDesc = d.getElementDescriptor(i)
                    val typeName = childDesc.serialName

                    map[typeName] = XmlPolymorphicUseDescriptorImpl(
                        this,
                        typeName,
                        qName,
                        xmlCodecBase,
                        childDesc,
                        parentTagDescriptor,
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

                    map[typeName] = XmlPolymorphicUseDescriptorImpl(
                        this,
                        typeName,
                        qName,
                        xmlCodecBase,
                        childDesc,
                        parentTagDescriptor,
                        outputKind
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
                NameInfo(parentTagDescriptor.serialDescriptor.serialName, parentTagDescriptor.tagName),
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
                            parentTagDescriptor.tagName.toNamespace()
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


    override fun asElement(tagName: QName): XmlDescriptor {
        return this
    }


    private val children = run {
        val overrideOutputKind: OutputKind? = when {
            transparent -> outputKind
            else        -> OutputKind.Element
        }
        List<XmlUseDescriptorImpl>(elementCount) { index ->
            XmlUseDescriptorImpl(
                this, index, xmlCodecBase, overrideOutputKind,
                parentTagDescriptor, serialDescriptor.getElementAnnotations(index)
                                )
        }
    }

    override fun getElementDescriptor(index: Int): XmlUseDescriptorImpl {
        return children[index]
    }

    fun getPolymorphicDescriptor(index: Int, serialDescriptor: SerialDescriptor): XmlUseDescriptor {
        return polyInfo2[serialDescriptor.serialName]
            ?: throw XmlSerialException("Missing polymorphic information for ${serialDescriptor.serialName}")
    }

    override fun <T> getChildDescriptor(index: Int, actualSerializer: SerializationStrategy<T>): XmlDescriptor {
        return getPolymorphicDescriptor(index, actualSerializer.descriptor).typeDescriptor

//        val child = polyInfo?.lookupName(serializer.descriptor.serialName)
//
//        return getElementDescriptor(index).typeDescriptor
        val childName = polyInfo.lookupName(actualSerializer.descriptor.serialName)?.name

        if (transparent) {

            // TODO probably use this to also record all possible polymorphic children

            return from(
                actualSerializer,
                xmlCodecBase,
                NameInfo(serialDescriptor.getElementName(1), childName),
                declParent,
                outputKind,
                useAnnotations = serialDescriptor.getElementAnnotations(1)
                       )
        } else {
            val valueName = NameInfo(serialDescriptor.getElementName(1), "value".toQname())
            return from(
                actualSerializer,
                xmlCodecBase,
                valueName,
                this,
                OutputKind.Element, // When not transparent it is always as an element for now
                useAnnotations = serialDescriptor.getElementAnnotations(1)
                       )

        }
    }

    override fun <T> getChildDescriptor(index: Int, actualDeserializer: DeserializationStrategy<T>): XmlDescriptor {
//        return getPolymorphicDescriptor(index, actualDeserializer.descriptor).typeDescriptor

        val childName = polyInfo.lookupName(actualDeserializer.descriptor.serialName)?.name

        if (transparent) {

            // TODO probably use this to also record all possible polymorphic children

            return from(
                actualDeserializer,
                xmlCodecBase,
                NameInfo(serialDescriptor.getElementName(1), childName),
                declParent,
                outputKind,
                useAnnotations = serialDescriptor.getElementAnnotations(1)
                       )
        } else {
            val valueName = NameInfo(serialDescriptor.getElementName(1), "value".toQname())
            return from(
                actualDeserializer,
                xmlCodecBase,
                valueName,
                this,
                OutputKind.Element, // When not transparent it is always as an element for now
                useAnnotations = serialDescriptor.getElementAnnotations(1)
                       )

        }
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

private fun SerialDescriptor.getNameInfo(): NameInfo {
    val realSerialName = when {
        isNullable && serialName.endsWith('?') -> serialName.dropLast(1)
        else                                   -> serialName
    }
    val qName = annotations.firstOrNull<XmlSerialName>()?.toQName()
    return NameInfo(realSerialName, qName)
}

class XmlListDescriptor internal constructor(
    serialDescriptor: SerialDescriptor,
    private val xmlCodecBase: XmlCodecBase,
    useNameInfo: NameInfo,
    tagName: QName,
    val useAnnotations: Collection<Annotation>,
    val childrenName: QName?,
    val anonymous: Boolean,
    override val parentTagDescriptor: XmlDescriptor
                                            ) : XmlDescriptor(serialDescriptor, xmlCodecBase, useNameInfo) {
    override val tagName = tagName
    private var childDescriptor: XmlDescriptor? = null

    val declParent get() = parentTagDescriptor

    override fun asElement(tagName: QName): XmlDescriptor {
        if (tagName.localPart.isEmpty()) throw XmlSerialException("Cannot serialize list without a name")
        return XmlListDescriptor(
            serialDescriptor,
            xmlCodecBase,
            useNameInfo,
            tagName,
            useAnnotations,
            childrenName,
            false,
            declParent
                                )
    }

    override val outputKind: OutputKind =
        if (useAnnotations.firstOrNull<XmlValue>() != null && xmlCodecBase.config.autoPolymorphic) OutputKind.Mixed else OutputKind.Element


    private val children = List<XmlUseDescriptorImpl>(elementCount) { index ->
        XmlUseDescriptorImpl(this, index, xmlCodecBase, outputKind, parentTagDescriptor, useAnnotations = useAnnotations)
    }

    override fun getElementDescriptor(index: Int): XmlUseDescriptorImpl {
        return children[index]
    }


/*
    override fun <T> getChildDescriptor(index: Int, serializer: SerializationStrategy<T>): XmlDescriptor {
        return childDescriptor ?: run {
            val useQName = when {
                childrenName == null && anonymous -> useAnnotations.firstOrNull<XmlSerialName>()?.toQName()

                else                              -> childrenName
            }
            val useSerialName = when {
                anonymous -> name.localPart
                else      -> serialDescriptor.getElementName(0)
            }
            val useName = XmlSerializationPolicy.NameInfo(useSerialName, useQName)

            from(
                serializer,
                xmlCodecBase,
                useName,
                declParent,
                outputKind,
                useAnnotations
                ).also {
                childDescriptor = it
            }
        }
    }
*/

/*
    override fun <T> getChildDescriptor(index: Int, deserializer: DeserializationStrategy<T>): XmlDescriptor {
        return childDescriptor ?: run {
            val useQName = when {
                childrenName == null && anonymous -> useAnnotations.firstOrNull<XmlSerialName>()?.toQName()

                else                              -> childrenName
            }
            val useSerialName = when {
                anonymous -> name.localPart
                else      -> serialDescriptor.getElementName(0)
            }
            val useName = XmlSerializationPolicy.NameInfo(useSerialName, useQName)

            from(
                deserializer,
                xmlCodecBase,
                useName,
                declParent,
                outputKind,
                useAnnotations
                ).also {
                childDescriptor = it
            }
        }
    }
*/
}
