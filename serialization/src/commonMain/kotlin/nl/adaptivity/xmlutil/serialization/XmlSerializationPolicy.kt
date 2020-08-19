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
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.DeclaredNameInfo
import nl.adaptivity.xmlutil.serialization.structure.SafeParentInfo
import nl.adaptivity.xmlutil.serialization.structure.XmlListDescriptor
import nl.adaptivity.xmlutil.serialization.structure.declOutputKind

interface XmlSerializationPolicy {

    val defaultPrimitiveOutputKind: OutputKind get() = OutputKind.Attribute
    val defaultObjectOutputKind: OutputKind get() = OutputKind.Element

    fun defaultOutputKind(serialKind: SerialKind): OutputKind = when (serialKind) {
        UnionKind.ENUM_KIND,
        StructureKind.OBJECT -> defaultObjectOutputKind
        is PrimitiveKind -> defaultPrimitiveOutputKind
        PolymorphicKind.OPEN -> OutputKind.Element
        else                 -> OutputKind.Element
    }

    fun invalidOutputKind(message: String) = ignoredSerialInfo(message)

    fun ignoredSerialInfo(message: String)

    fun effectiveName(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        outputKind: OutputKind,
        useName: DeclaredNameInfo = tagParent.elementUseNameInfo
                     ): QName

    fun isListEluded(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): Boolean
    fun isTransparentPolymorphic(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): Boolean


    fun serialNameToQName(serialName: String, parentNamespace: Namespace): QName

    data class DeclaredNameInfo(val serialName: String, val annotatedName: QName?)
    data class ActualNameInfo(val serialName: String, val annotatedName: QName)

    fun effectiveOutputKind(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): OutputKind

    fun handleUnknownContent(input: XmlReader, inputKind: InputKind, name: QName?, candidates: Collection<Any>)
}

open class DefaultXmlSerializationPolicy(
    val pedantic: Boolean,
    val autoPolymorphic: Boolean = false,
    private val unknownChildHandler: UnknownChildHandler = XmlConfig.DEFAULT_UNKNOWN_CHILD_HANDLER
                                        ) : XmlSerializationPolicy {

    override fun isListEluded(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): Boolean {
        val useAnnotations = tagParent.elementUseAnnotations
        val isMixed = useAnnotations.firstOrNull<XmlValue>()?.value == true
        if (isMixed) return true

        val reqChildrenName = useAnnotations.firstOrNull<XmlChildrenName>()?.toQName()
        return reqChildrenName == null // TODO use the policy
    }

    override fun isTransparentPolymorphic(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): Boolean {
        val xmlPolyChildren = tagParent.elementUseAnnotations.firstOrNull<XmlPolyChildren>()
        return autoPolymorphic || xmlPolyChildren != null
    }

    override fun effectiveOutputKind(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): OutputKind {
        val serialDescriptor = serializerParent.elementSerialDescriptor

        return when (val overrideOutputKind = serializerParent.elementUseOutputKind) {
            null -> {
                val useAnnotations = tagParent.elementUseAnnotations
                val isValue = useAnnotations.firstOrNull<XmlValue>()?.value == true
                val elementKind = tagParent.elementSerialDescriptor.kind
                when {
                    elementKind == StructureKind.CLASS -> OutputKind.Element
                    isValue -> OutputKind.Mixed
                    else    -> tagParent.elementUseOutputKind ?: serialDescriptor.declOutputKind()
                    ?: defaultOutputKind(serialDescriptor.kind)
                }
            }
            OutputKind.Mixed -> {
                if (serializerParent.descriptor is XmlListDescriptor) {
                    if (tagParent.elementSerialDescriptor.kind == StructureKind.CLASS) {
                        OutputKind.Element
                    } else {
                        OutputKind.Mixed
                    }
                } else {
                    val outputKind = tagParent.elementUseOutputKind
                        ?: serialDescriptor.declOutputKind()
                        ?: defaultOutputKind(serialDescriptor.kind)

                    when (outputKind) {
                        OutputKind.Attribute -> OutputKind.Text
                        else                 -> outputKind
                    }
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
        useName: DeclaredNameInfo
                              ): QName {
        val typeDescriptor = serializerParent.elemenTypeDescriptor
        val serialKind = typeDescriptor.serialDescriptor.kind
        val typeNameInfo = typeDescriptor.typeNameInfo
        val parentNamespace: Namespace = tagParent.namespace

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
                    typeNameInfo.serialName == "kotlin.Unit" || // Unit needs a special case
                    parentSerialKind is PolymorphicKind // child of explict polymorphic uses predefined names
                                               -> serialNameToQName(useName.serialName, parentNamespace)

            typeNameInfo.annotatedName != null -> typeNameInfo.annotatedName

            else                               -> serialNameToQName(typeNameInfo.serialName, parentNamespace)
        }
    }

    override fun handleUnknownContent(
        input: XmlReader,
        inputKind: InputKind,
        name: QName?,
        candidates: Collection<Any>
                                     ) {
        unknownChildHandler(input, inputKind, name, candidates)
    }

    override fun ignoredSerialInfo(message: String) {
        if (pedantic) throw XmlSerialException(message)
    }
}