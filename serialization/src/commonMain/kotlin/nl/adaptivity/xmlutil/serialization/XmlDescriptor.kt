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

private fun SerialDescriptor.declDefault(): String? =
    annotations.firstOrNull<XmlDefault>()?.value

private fun SerialDescriptor.declOutputKind(): OutputKind? {
    for(a in annotations) {
        when (a) {
                is XmlValue -> return OutputKind.Text
                is XmlElement -> return if (a.value) OutputKind.Element else OutputKind.Attribute
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

    open val childCound: Int get() = 0

    open fun <T> getChildDescriptor(index: Int, serializer: SerializationStrategy<T>): XmlDescriptor {
        throw IndexOutOfBoundsException("There are no children")
    }

    companion object {
        fun from(
            serializer: SerializationStrategy<*>,
            xml: XML,
            useName: XmlSerializationPolicy.NameInfo,
            parentNamespace: Namespace,
            overrideOutputKind: OutputKind? = null,
            useDefault: String? = null
                ): XmlDescriptor {
            val policy = xml.config.policy

            val d = serializer.descriptor

            val effectiveDefault = useDefault ?: d.declDefault()
            val effectiveOutputKind = overrideOutputKind ?: d.declOutputKind() ?: policy.defaultOutputKind(d.kind)

            val name = policy.effectiveName(d.kind, effectiveOutputKind, parentNamespace, useName, d.getNameInfo())

            when (d.kind) {
                is PrimitiveKind -> return XmlPrimitiveDescriptor(d, name, effectiveOutputKind, effectiveDefault)
//                StructureKind.LIST -> TODO("LIST")
//                StructureKind.MAP -> TODO("MAP")
                is PolymorphicKind -> return XmlPolymorphicDescriptor(d, name, xml, effectiveDefault)
                else -> return XmlCompositeDescriptor(d, name, xml, effectiveDefault)
            }

            TODO("Not implemented yet: ${d.kind}")
        }

        fun from(
            deserializer: DeserializationStrategy<*>,
            xml: XML
                ): XmlDescriptor = TODO("Not implemented yet")
    }
}

sealed class XmlValueDescriptor(
    serialDescriptor: SerialDescriptor,
    name: QName,
    val default: String?
                               ) : XmlDescriptor(serialDescriptor, name)

class XmlPrimitiveDescriptor(
    serialDescriptor: SerialDescriptor,
    name: QName,
    val outputKind: OutputKind,
    default: String? = null
                            ) : XmlValueDescriptor(serialDescriptor, name, default) {
    init {
        assert(outputKind != OutputKind.Mixed) { "It is not valid to have a value of mixed output type" }
    }

    override fun asElement(name: QName): XmlDescriptor =
        XmlPrimitiveDescriptor(serialDescriptor, name, OutputKind.Element, default)
}

class XmlCompositeDescriptor(
    serialDescriptor: SerialDescriptor,
    name: QName,
    val xml: XML,
    default: String? = null
                            ) : XmlValueDescriptor(serialDescriptor, name, default) {
    private val children: MutableList<XmlDescriptor?> = MutableList(serialDescriptor.elementsCount) { null }
    override val childCound: Int get() = children.size

    override fun <T> getChildDescriptor(index: Int, serializer: SerializationStrategy<T>): XmlDescriptor {
        return children[index] ?: run {
            val useName = serialDescriptor.getElementNameInfo(index)
            val useOutputKind = serialDescriptor.getElementAnnotations(index).getRequestedOutputKind()
            from(serializer, xml, useName, name.toNamespace(),useOutputKind, serialDescriptor.getElementDefault(index)).also {
                children[index] = it
            }
        }
    }

    override fun asElement(name: QName): XmlDescriptor = this
}

class XmlPolymorphicDescriptor(
    serialDescriptor: SerialDescriptor,
    name: QName,
    val xml: XML,
    default: String? = null
                              ) : XmlValueDescriptor(serialDescriptor, name, default) {

    private val typeDescriptor = XmlDescriptor.from(String.serializer(), xml, serialDescriptor.getElementNameInfo(0), name.toNamespace())

    override fun asElement(name: QName): XmlDescriptor {
        return this
    }

    override fun <T> getChildDescriptor(index: Int, serializer: SerializationStrategy<T>): XmlDescriptor {
        if (index == 0) return typeDescriptor

        // TODO probably use this to also record all possible polymorphic children

        return from(serializer, xml, serialDescriptor.getElementNameInfo(1), name.toNamespace())
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

class XmlListDescriptor(
    serialDescriptor: SerialDescriptor,
    name: QName,
    val xml: XML,
    val anonymous: Boolean = name.localPart.isEmpty()
                       ) : XmlDescriptor(serialDescriptor, name) {
    private var childDescriptor: XmlDescriptor? = null

    override fun asElement(name: QName): XmlDescriptor {
        if (name.localPart.isEmpty()) throw XmlSerialException("Cannot serialize list without a name")
        return XmlListDescriptor(serialDescriptor, name, xml, false)
    }

    override fun <T> getChildDescriptor(index: Int, serializer: SerializationStrategy<T>): XmlDescriptor {
        return childDescriptor ?: run {
            val useName = if (anonymous) {
                serialDescriptor.getNameInfo()
            } else {
                XmlSerializationPolicy.NameInfo(
                    serialDescriptor.getElementName(0),
                    serialDescriptor.annotations.firstOrNull<XmlChildrenName>()?.toQName()
                                               )
            }
            from(serializer, xml, useName, name.toNamespace(), OutputKind.Element).also {
                childDescriptor = it
            }
        }
    }
}

