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

    open fun <T> getChildDescriptor(index: Int, serializer: SerializationStrategy<T>): XmlDescriptor {
        throw IndexOutOfBoundsException("There are no children")
    }

    companion object {
        internal fun from(
            serializer: SerializationStrategy<*>,
            xmlCodecBase: XmlCodecBase,
            useName: XmlSerializationPolicy.NameInfo,
            parent: XmlDescriptor,
            overrideOutputKind: OutputKind? = null,
            useAnnotations: Collection<Annotation>
                         ): XmlDescriptor {
            val policy = xmlCodecBase.config.policy
            val parentNamespace = parent.name.toNamespace()

            val useDefault: String? = useAnnotations.firstOrNull<XmlDefault>()?.value

            val d = serializer.descriptor

            val effectiveDefault = useDefault ?: d.declDefault()
            val isValue = useAnnotations.firstOrNull<XmlValue>()?.value == true
            val effectiveOutputKind = overrideOutputKind ?: if (!isValue) {
                d.declOutputKind() ?: policy.defaultOutputKind(d.kind)
            } else if (parent.serialDescriptor.kind == StructureKind.LIST) { // XmlValue
                OutputKind.Mixed
            } else {
                OutputKind.Text
            }

            val name = policy.effectiveName(d.kind, effectiveOutputKind, parentNamespace, useName, d.getNameInfo())

            when (d.kind) {
                UnionKind.ENUM_KIND,
                is PrimitiveKind   -> return XmlPrimitiveDescriptor(d, name, if (effectiveOutputKind == OutputKind.Mixed) OutputKind.Text else effectiveOutputKind, effectiveDefault)
                StructureKind.LIST -> {
                    val isMixed = useAnnotations.firstOrNull<XmlValue>()?.value == true
                    val reqChildrenName = useAnnotations.firstOrNull<XmlChildrenName>()?.toQName()
                    val isListAnonymous = isMixed || reqChildrenName == null // TODO use the policy

                    val childrenName =
                        reqChildrenName ?: when {
                            isListAnonymous -> useAnnotations.firstOrNull<XmlSerialName>()?.toQName()
                            else            -> null
                        }

                    return XmlListDescriptor(d, name, xmlCodecBase, useAnnotations, childrenName, isListAnonymous)
                }
                //                StructureKind.MAP -> TODO("MAP")
                is PolymorphicKind -> {
                    val polyChildren = useAnnotations.firstOrNull<XmlPolyChildren>()
                    return XmlPolymorphicDescriptor(
                        d,
                        name,
                        xmlCodecBase,
                        polyChildren,
                        parent,
                        (serializer as? PolymorphicSerializer)?.baseClass,
                        effectiveOutputKind
                                                   )
                }
                else               -> return XmlCompositeDescriptor(d, name, xmlCodecBase, effectiveDefault)
            }
        }

        fun from(
            deserializer: DeserializationStrategy<*>,
            xml: XML
                ): XmlDescriptor = TODO("Not implemented yet")
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
                                                 ) : XmlValueDescriptor(serialDescriptor, name, OutputKind.Element, default) {
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
    parent: XmlDescriptor,
    val baseClass: KClass<*>?,
    outputKind: OutputKind
                                                   ) : XmlValueDescriptor(serialDescriptor, name, outputKind, null) {
    // xmlPolyChildren and sealed also leads to a transparent polymorphic
    val transparent = xmlCodecBase.config.autoPolymorphic || xmlPolyChildren!=null


    private val typeDescriptor = XmlDescriptor.from(
        String.serializer(),
        xmlCodecBase,
        serialDescriptor.getElementNameInfo(0),
        this,
        outputKind,
        useAnnotations = serialDescriptor.getElementAnnotations(0)
                                                   )

    private val polyInfo: XmlNameMap? = when {
        xmlPolyChildren != null
                                                        -> {
            val baseClass = baseClass ?: Any::class
            xmlCodecBase.polyInfo(
                XmlSerializationPolicy.NameInfo(parent.serialDescriptor.serialName, parent.name),
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

                    val effectiveChildName = childNameInfo.annotatedName?:xmlCodecBase.config.policy.serialNameToQName(childDesc.serialName, parent.name.toNamespace())
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
                this,
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
}

private fun SerialDescriptor.getElementDefault(index: Int): String? {
    return getElementAnnotations(index).firstOrNull<XmlDefault>()?.value
}

private fun SerialDescriptor.getElementNameInfo(index: Int): XmlSerializationPolicy.NameInfo {
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
    val anonymous: Boolean
                                            ) : XmlDescriptor(serialDescriptor, name) {
    private var childDescriptor: XmlDescriptor? = null

    override fun asElement(name: QName): XmlDescriptor {
        if (name.localPart.isEmpty()) throw XmlSerialException("Cannot serialize list without a name")
        return XmlListDescriptor(serialDescriptor, name, xmlCodecBase, useAnnotations, childrenName, false)
    }

    override val outputKind: OutputKind = if (useAnnotations.firstOrNull<XmlValue>()!=null) OutputKind.Mixed else OutputKind.Element

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
                this,
                outputKind,
                useAnnotations
                ).also {
                childDescriptor = it
            }
        }
    }
}

