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
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.toQname

interface XmlSerializationPolicy {
/*
    fun effectiveOutputKind(
        parentDescriptor: SerialDescriptor,
        isValueChild: Boolean,
        index: Int,
        serialDescriptor: SerialDescriptor = parentDescriptor.getElementDescriptor(index),
        useRequestedKind: OutputKind? = parentDescriptor.getUseRequestedKind(index),
        typeRequestedKind: OutputKind? = parentDescriptor.getTypeRequestedKind(index)
                           ): OutputKind
*/

    val defaultPrimitiveOutputKind: OutputKind get() = OutputKind.Attribute

    fun defaultOutputKind(serialKind: SerialKind): OutputKind = when (serialKind) {
        UnionKind.ENUM_KIND,
        StructureKind.OBJECT,
        is PrimitiveKind -> defaultPrimitiveOutputKind
        else -> OutputKind.Element
    }

    fun ignoredSerialInfo(message: String)

    fun effectiveName(
        serialKind: SerialKind,
        outputKind: OutputKind,
        parentNamespace: Namespace,
        useName: NameInfo,
        declName: NameInfo
                     ): QName

    fun serialNameToQName(serialName: String, parentNamespace: Namespace): QName

    data class NameInfo(val serialName: String, val annotatedName: QName?)
}

private fun SerialDescriptor.getUseRequestedKind(index: Int): OutputKind? {
    // handle incomplete descriptors, including list and map descriptors
    if (index >= elementsCount) return null
    return getElementAnnotations(index).getRequestedOutputKind()
}

private fun SerialDescriptor.getTypeRequestedKind(index: Int): OutputKind? {
    // handle incomplete descriptors, including list and map descriptors
    if (index >= elementsCount) return null
    return getElementDescriptor(index).annotations.getRequestedOutputKind()
}

internal fun <T : Annotation> Iterable<T>.getRequestedOutputKind(): OutputKind? {
    for (annotation in this) {
        when (annotation) {
            is XmlValue        -> return OutputKind.Text
            is XmlElement      -> return if (annotation.value) OutputKind.Element else OutputKind.Attribute
            is XmlPolyChildren,
            is XmlChildrenName -> return OutputKind.Element
        }
    }
    return null
}

object DefaultXmlSerializationPolicy : BaseXmlSerializationPolicy(pedantic = false)

open class BaseXmlSerializationPolicy(val pedantic: Boolean) : XmlSerializationPolicy {
    override fun serialNameToQName(serialName: String, parentNamespace: Namespace): QName {
        return serialName.substringAfterLast('.').toQname(parentNamespace)
    }

    override fun effectiveName(
        serialKind: SerialKind,
        outputKind: OutputKind,
        parentNamespace: Namespace,
        useName: XmlSerializationPolicy.NameInfo,
        declName: XmlSerializationPolicy.NameInfo
                              ): QName {
        return when {
            useName.annotatedName != null      -> useName.annotatedName
            serialKind == StructureKind.MAP ||
            serialKind == StructureKind.LIST ||
            outputKind == OutputKind.Attribute -> serialNameToQName(useName.serialName, parentNamespace)
            declName.annotatedName != null -> declName.annotatedName
            else -> serialNameToQName(declName.serialName, parentNamespace)
        }
    }

    /*
    override fun effectiveOutputKind(
        parentDescriptor: SerialDescriptor,
        isValueChild: Boolean,
        index: Int,
        serialDescriptor: SerialDescriptor,
        useRequestedKind: OutputKind?,
        typeRequestedKind: OutputKind?
                            ): OutputKind {
        val parentKind: SerialKind = parentDescriptor.kind
        val serialKind: SerialKind = serialDescriptor.kind
        // The children of these are always elements

        when (parentKind) {
            StructureKind.LIST,
            StructureKind.MAP,
            is PolymorphicKind -> return OutputKind.Element
        }


        if (isValueChild) {
            return when (serialKind) {
                is PrimitiveKind      -> OutputKind.Text
                is PolymorphicKind,
                is StructureKind.LIST -> OutputKind.Mixed
                else                  -> OutputKind.Element//throw XmlSerialException("@XmlValue annotations can only be put on primitive and list types, not ${childDesc?.kind}")
            }
        }

        when (useRequestedKind) {
            OutputKind.Element -> return OutputKind.Element
            OutputKind.Attribute -> {
                when (serialKind) {
                    is StructureKind,
                    is PolymorphicKind,
                    UnionKind.CONTEXTUAL -> {
                        ignoredSerialInfo("The use site for ${serialDescriptor} requests $useRequestedKind, but the type $serialKind does not support this")
                    }
                }
            }
        }
        if (useRequestedKind == OutputKind.Element)

        if (index < elementsCount) {// This can be false for lists, they are always elements anyway
            for (annotation in getElementAnnotations(index)) {
                when (annotation) {
                    is XmlChildrenName -> return OutputKind.Element.checkValueChild()
                    is XmlElement      -> return if (annotation.value) OutputKind.Element.checkValueChild() else OutputKind.Attribute
                }
            }
        }

        // For lists, the parent is used for the name (but should not be used for type)
        when (getElementDescriptor(index).kind) {
            StructureKind.LIST,
            StructureKind.MAP -> return OutputKind.Element.checkValueChild()
        }

        // Lists are always elements
        if (childDesc != null) {
            if (childDesc.kind is StructureKind) return OutputKind.Element.checkValueChild()
            childDesc.annotations.firstOrNull<XmlElement>()
                ?.let { if (it.value) return OutputKind.Element.checkValueChild() else OutputKind.Attribute }
        }

        return when (childDesc?.kind) {
            null,
            is PrimitiveKind -> OutputKind.Attribute
            else             -> OutputKind.Element.checkValueChild()
        }

        TODO()
    }
*/

    override fun ignoredSerialInfo(message: String) {
        if (pedantic) throw XmlSerialException(message)
    }
}