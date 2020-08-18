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
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor

interface XmlSerializationPolicy {

    val defaultPrimitiveOutputKind: OutputKind get() = OutputKind.Attribute
    val defaultObjectOutputKind: OutputKind get() = OutputKind.Element

    fun defaultOutputKind(serialKind: SerialKind): OutputKind = when (serialKind) {
        UnionKind.ENUM_KIND,
        StructureKind.OBJECT -> defaultObjectOutputKind
        is PrimitiveKind -> defaultPrimitiveOutputKind
        PolymorphicKind.OPEN -> OutputKind.Element
        else -> OutputKind.Element
    }

    fun ignoredSerialInfo(message: String)

    fun effectiveName(
        serialKind: SerialKind,
        outputKind: OutputKind,
        tagParent: XmlDescriptor,
        useName: NameInfo,
        declName: NameInfo,
        parentNamespace: Namespace = tagParent.tagName.toNamespace()
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
            is XmlValue        -> return OutputKind.Mixed
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
        tagParent: XmlDescriptor,
        useName: XmlSerializationPolicy.NameInfo,
        declName: XmlSerializationPolicy.NameInfo,
        parentNamespace: Namespace
                              ): QName {

        return when {
            useName.annotatedName != null      -> useName.annotatedName

            outputKind == OutputKind.Attribute -> QName(useName.serialName) // Use non-prefix attributes by default

            serialKind is PrimitiveKind ||
            serialKind == StructureKind.MAP ||
            serialKind == StructureKind.LIST ||
            serialKind == PolymorphicKind.OPEN ||
            declName.serialName=="kotlin.Unit" || // Unit needs a special case
            tagParent.serialKind is PolymorphicKind // child of explict polymorphic uses predefined names
            -> serialNameToQName(useName.serialName, parentNamespace)

            declName.annotatedName != null -> declName.annotatedName

            else -> serialNameToQName(declName.serialName, parentNamespace)
        }
    }

    override fun ignoredSerialInfo(message: String) {
        if (pedantic) throw XmlSerialException(message)
    }
}