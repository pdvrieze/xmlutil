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

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import nl.adaptivity.serialutil.impl.assert
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.canary.polyBaseClassName
import nl.adaptivity.xmlutil.serialization.impl.ChildCollector
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
    val serialDescriptor: SerialDescriptor,
    val name: QName
                          ) {
    abstract fun asElement(name: QName = this.name): XmlDescriptor
    abstract val outputKind: OutputKind

    open val childCound: Int get() = 0
    val serialKind: SerialKind get() = serialDescriptor.kind

    open fun <T> getChildDescriptor(index: Int, serializer: SerializationStrategy<T>): XmlDescriptor {
        throw IndexOutOfBoundsException("There are no children")
    }

    open fun <T> getChildDescriptor(index: Int, serializer: KSerializer<T>): XmlDescriptor {
        return getChildDescriptor(index, deserializer = serializer)
    }

    open fun <T> getChildDescriptor(index: Int, deserializer: DeserializationStrategy<T>): XmlDescriptor {
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
            useName: XmlSerializationPolicy.NameInfo,
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
            useName: XmlSerializationPolicy.NameInfo,
            declParent: XmlDescriptor,
            overrideOutputKind: OutputKind? = null,
            useAnnotations: Collection<Annotation> = emptyList()
                         ): XmlDescriptor {
            val baseClass = (serializer as? PolymorphicSerializer)?.baseClass

            return fromCommon(serializer.descriptor, xmlCodecBase, useName, declParent, overrideOutputKind, useAnnotations, baseClass)
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
            useName: XmlSerializationPolicy.NameInfo,
            declParent: XmlDescriptor,
            overrideOutputKind: OutputKind? = null,
            useAnnotations: Collection<Annotation> = emptyList()
                         ): XmlDescriptor {
            val baseClass = (deserializer as? PolymorphicSerializer)?.baseClass

            return fromCommon(deserializer.descriptor, xmlCodecBase, useName, declParent, overrideOutputKind, useAnnotations, baseClass)
        }

        private fun fromCommon(
            serialDescriptor: SerialDescriptor,
            xmlCodecBase: XmlCodecBase,
            useName: XmlSerializationPolicy.NameInfo,
            declParent: XmlDescriptor,
            overrideOutputKind: OutputKind?,
            useAnnotations: Collection<Annotation>,
            baseClass: KClass<*>?
                              ): XmlDescriptor {
            val policy = xmlCodecBase.config.policy
            val parentNamespace = declParent.name.toNamespace()

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
                useName,
                serialDescriptor.getNameInfo()
                                           )

            when (serialDescriptor.kind) {
                UnionKind.ENUM_KIND,
                is PrimitiveKind   -> return XmlPrimitiveDescriptor(
                    serialDescriptor,
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
                        name,
                        xmlCodecBase,
                        useAnnotations,
                        childrenName,
                        isListAnonymous,
                        declParent
                                            )
                }
                //                StructureKind.MAP -> TODO("MAP")
                is PolymorphicKind -> {
                    val polyChildren = useAnnotations.firstOrNull<XmlPolyChildren>()
                    return XmlPolymorphicDescriptor(
                        serialDescriptor,
                        name,
                        xmlCodecBase,
                        polyChildren,
                        declParent,
                        baseClass,
                        effectiveOutputKind
                                                   )
                }
                else               -> return XmlCompositeDescriptor(
                    serialDescriptor,
                    name,
                    xmlCodecBase,
                    effectiveDefault
                                                                   )
            }
        }
    }
}

class XmlRootDescriptor(name: QName, descriptor: SerialDescriptor) : XmlDescriptor(descriptor, name) {
    override val outputKind: OutputKind get() = OutputKind.Mixed

    override fun asElement(name: QName): XmlDescriptor =
        throw UnsupportedOperationException("A dummy parent is not an element")


}

sealed class XmlValueDescriptor(
    serialDescriptor: SerialDescriptor,
    name: QName,
    override val outputKind: OutputKind,
    val default: String?
                               ) : XmlDescriptor(serialDescriptor, name)

class XmlPrimitiveDescriptor(
    serialDescriptor: SerialDescriptor,
    name: QName,
    outputKind: OutputKind,
    default: String? = null
                            ) : XmlValueDescriptor(serialDescriptor, name, outputKind, default) {
    init {
        assert(outputKind != OutputKind.Mixed) { "It is not valid to have a value of mixed output type" }
    }

    override fun asElement(name: QName): XmlDescriptor =
        XmlPrimitiveDescriptor(serialDescriptor, name, OutputKind.Element, default)
}

class XmlCompositeDescriptor internal constructor(
    serialDescriptor: SerialDescriptor,
    name: QName,
    private val xmlCodec: XmlCodecBase,
    default: String? = null
                                                 ) :
    XmlValueDescriptor(serialDescriptor, name, OutputKind.Element, default) {
    private val children: MutableList<XmlDescriptor?> = MutableList(serialDescriptor.elementsCount) { null }
    override val childCound: Int get() = children.size

    override fun <T> getChildDescriptor(index: Int, serializer: SerializationStrategy<T>): XmlDescriptor {
        return children[index] ?: run {
            val useName = serialDescriptor.getElementNameInfo(index)
            val useOutputKind = serialDescriptor.getElementAnnotations(index).getRequestedOutputKind()
            from(
                serializer,
                xmlCodec,
                useName,
                this,
                useOutputKind,
                serialDescriptor.getElementAnnotations(index)
                ).also {
                children[index] = it
            }
        }
    }

    override fun <T> getChildDescriptor(index: Int, deserializer: DeserializationStrategy<T>): XmlDescriptor {
        return children[index] ?: run {
            val useName = serialDescriptor.getElementNameInfo(index)
            val useOutputKind = serialDescriptor.getElementAnnotations(index).getRequestedOutputKind()
            from(
                deserializer,
                xmlCodec,
                useName,
                this,
                useOutputKind,
                serialDescriptor.getElementAnnotations(index)
                ).also {
                children[index] = it
            }
        }
    }

    override fun asElement(name: QName): XmlDescriptor = this

    override fun toString(): String {
        return children.joinToString(",\n", "${name}: (\n", ")") { it.toString().prependIndent("    ") }
    }
}

class XmlPolymorphicDescriptor internal constructor(
    serialDescriptor: SerialDescriptor,
    name: QName,
    private val xmlCodecBase: XmlCodecBase,
    xmlPolyChildren: XmlPolyChildren?,
    private val declParent: XmlDescriptor,
    val baseClass: KClass<*>?,
    outputKind: OutputKind
                                                   ) : XmlValueDescriptor(serialDescriptor, name, outputKind, null) {
    // xmlPolyChildren and sealed also leads to a transparent polymorphic
    val transparent = xmlCodecBase.config.autoPolymorphic || xmlPolyChildren != null

    val parentSerialName = declParent.serialDescriptor.serialName

    private val typeDescriptor = from(
        String.serializer(),
        xmlCodecBase,
        serialDescriptor.getElementNameInfo(0),
        declParent,
        outputKind,
        useAnnotations = serialDescriptor.getElementAnnotations(0)
                                     )

    private val polyInfo: XmlNameMap? = when {
        xmlPolyChildren != null
                                                        -> {
            val baseClass = baseClass ?: Any::class
            xmlCodecBase.polyInfo(
                XmlSerializationPolicy.NameInfo(declParent.serialDescriptor.serialName, declParent.name),
                xmlPolyChildren.value,
                baseClass
                                 )
        }
        !xmlCodecBase.config.autoPolymorphic            -> null // Don't help for the non-auto case
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
                            declParent.name.toNamespace()
                                                                       )
                    registerClass(effectiveChildName, d.getElementDescriptor(i).serialName, true)
                }
            }
        }

        baseClass != null
                                                        -> {
            val childCollector = ChildCollector(baseClass)
            xmlCodecBase.context.dumpTo(childCollector)
            childCollector.getPolyInfo(name)
        }

        serialDescriptor.kind is PolymorphicKind.OPEN   -> {
            val childCollector =
                serialDescriptor.polyBaseClassName?.let { ChildCollector(it) } ?: ChildCollector(Any::class)
            xmlCodecBase.context.dumpTo(childCollector)

            childCollector.getPolyInfo(name)
        }

        else                                            -> null
    }


    override fun asElement(name: QName): XmlDescriptor {
        return this
    }

    override fun <T> getChildDescriptor(index: Int, serializer: SerializationStrategy<T>): XmlDescriptor {
        val childName = polyInfo?.lookupName(serializer.descriptor.serialName)?.name

        if (transparent) {

            // TODO probably use this to also record all possible polymorphic children

            return from(
                serializer,
                xmlCodecBase,
                XmlSerializationPolicy.NameInfo(serialDescriptor.getElementName(1), childName),
                declParent,
                outputKind,
                useAnnotations = serialDescriptor.getElementAnnotations(1)
                       )
        } else {
            val valueName = XmlSerializationPolicy.NameInfo(serialDescriptor.getElementName(1), "value".toQname())
            return from(
                serializer,
                xmlCodecBase,
                valueName,
                this,
                OutputKind.Element, // When not transparent it is always as an element for now
                useAnnotations = serialDescriptor.getElementAnnotations(1)
                       )

        }
    }

    override fun <T> getChildDescriptor(index: Int, deserializer: DeserializationStrategy<T>): XmlDescriptor {
        val childName = polyInfo?.lookupName(deserializer.descriptor.serialName)?.name

        if (transparent) {

            // TODO probably use this to also record all possible polymorphic children

            return from(
                deserializer,
                xmlCodecBase,
                XmlSerializationPolicy.NameInfo(serialDescriptor.getElementName(1), childName),
                declParent,
                outputKind,
                useAnnotations = serialDescriptor.getElementAnnotations(1)
                       )
        } else {
            val valueName = XmlSerializationPolicy.NameInfo(serialDescriptor.getElementName(1), "value".toQname())
            return from(
                deserializer,
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

internal fun SerialDescriptor.getElementNameInfo(index: Int): XmlSerializationPolicy.NameInfo {
    val serialName = getElementName(index)
    val qName = getElementAnnotations(index).firstOrNull<XmlSerialName>()?.toQName()
    return XmlSerializationPolicy.NameInfo(serialName, qName)
}

private fun SerialDescriptor.getNameInfo(): XmlSerializationPolicy.NameInfo {
    val qName = annotations.firstOrNull<XmlSerialName>()?.toQName()
    return XmlSerializationPolicy.NameInfo(this.serialName, qName)
}

class XmlListDescriptor internal constructor(
    serialDescriptor: SerialDescriptor,
    name: QName,
    private val xmlCodecBase: XmlCodecBase,
    val useAnnotations: Collection<Annotation>,
    val childrenName: QName?,
    val anonymous: Boolean,
    private val declParent: XmlDescriptor
                                            ) : XmlDescriptor(serialDescriptor, name) {
    private var childDescriptor: XmlDescriptor? = null

    override fun asElement(name: QName): XmlDescriptor {
        if (name.localPart.isEmpty()) throw XmlSerialException("Cannot serialize list without a name")
        return XmlListDescriptor(serialDescriptor, name, xmlCodecBase, useAnnotations, childrenName, false, declParent)
    }

    override val outputKind: OutputKind =
        if (useAnnotations.firstOrNull<XmlValue>() != null) OutputKind.Mixed else OutputKind.Element

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
}

