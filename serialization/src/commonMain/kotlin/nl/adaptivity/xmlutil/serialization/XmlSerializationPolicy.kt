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
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.serialization.structure.SafeParentInfo
import nl.adaptivity.xmlutil.serialization.structure.XmlListDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlTypeDescriptor
import nl.adaptivity.xmlutil.serialization.structure.declOutputKind

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
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        outputKind: OutputKind,
        useName: DeclaredNameInfo = tagParent.elementUseNameInfo,
        typeDescriptor: XmlTypeDescriptor,
        serialKind: SerialKind = tagParent.elementSerialDescriptor.kind,
        typeNameInfo: DeclaredNameInfo = typeDescriptor.typeNameInfo,
        parentNamespace: Namespace = tagParent.namespace
                     ): QName

    fun serialNameToQName(serialName: String, parentNamespace: Namespace): QName

    data class DeclaredNameInfo(val serialName: String, val annotatedName: QName?)
    data class ActualNameInfo(val serialName: String, val annotatedName: QName)
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


    fun XmlSerializationPolicy.determineOutputKind(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
                                                  ): OutputKind {
        val serialDescriptor = serializerParent.elementSerialDescriptor

        return when (val overrideOutputKind = serializerParent.elementUseOutputKind) {
            null             -> {
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


    override fun serialNameToQName(serialName: String, parentNamespace: Namespace): QName {
        return serialName.substringAfterLast('.').toQname(parentNamespace)
    }

    override fun effectiveName(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        outputKind: OutputKind,
        useName: XmlSerializationPolicy.DeclaredNameInfo,
        typeDescriptor: XmlTypeDescriptor,
        serialKind: SerialKind,
        typeNameInfo: XmlSerializationPolicy.DeclaredNameInfo,
        parentNamespace: Namespace
                              ): QName {
        assert(typeNameInfo == typeDescriptor.typeNameInfo) {
            "Type name info should match"
        }

        val parentSerialKind = tagParent.descriptor?.serialKind

        return when {
            useName.annotatedName != null      -> useName.annotatedName

            outputKind == OutputKind.Attribute -> QName(useName.serialName) // Use non-prefix attributes by default

            serialKind is PrimitiveKind ||
            serialKind == StructureKind.MAP ||
            serialKind == StructureKind.LIST ||
            serialKind == PolymorphicKind.OPEN ||
            typeNameInfo.serialName=="kotlin.Unit" || // Unit needs a special case
            parentSerialKind is PolymorphicKind // child of explict polymorphic uses predefined names
                                               -> serialNameToQName(useName.serialName, parentNamespace)

            typeNameInfo.annotatedName != null -> typeNameInfo.annotatedName

            else                               -> serialNameToQName(typeNameInfo.serialName, parentNamespace)
        }
    }

    override fun ignoredSerialInfo(message: String) {
        if (pedantic) throw XmlSerialException(message)
    }
}