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
@file:OptIn(XmlUtilInternal::class)

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.DeclaredNameInfo
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.XmlEncodeDefault
import nl.adaptivity.xmlutil.serialization.structure.*
import nl.adaptivity.xmlutil.serialization.structure.declOutputKind

interface XmlSerializationPolicy {

    val defaultPrimitiveOutputKind: OutputKind get() = OutputKind.Attribute
    val defaultObjectOutputKind: OutputKind get() = OutputKind.Element

    @ExperimentalSerializationApi
    fun defaultOutputKind(serialKind: SerialKind): OutputKind =
        when (serialKind) {
            SerialKind.ENUM,
            StructureKind.OBJECT -> defaultObjectOutputKind
            is PrimitiveKind     -> defaultPrimitiveOutputKind
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

    fun isListEluded(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
                    ): Boolean

    fun isTransparentPolymorphic(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
                                ): Boolean

    @Suppress("DEPRECATION")
    fun serialTypeNameToQName(
        typeNameInfo: DeclaredNameInfo,
        parentNamespace: Namespace
                             ): QName =
        serialNameToQName(typeNameInfo.serialName, parentNamespace)

    @Suppress("DEPRECATION")
    fun serialUseNameToQName(
        useNameInfo: DeclaredNameInfo,
        parentNamespace: Namespace
                            ): QName =
        serialNameToQName(useNameInfo.serialName, parentNamespace)

    @Deprecated("It is recommended to override serialTypeNameToQName and serialUseNameToQName instead")
    fun serialNameToQName(
        serialName: String,
        parentNamespace: Namespace
                         ): QName

    data class DeclaredNameInfo(
        val serialName: String,
        val annotatedName: QName?
                               )

    data class ActualNameInfo(
        val serialName: String,
        val annotatedName: QName
                             )

    @Deprecated("Don't use or implement this, use the 3 parameter version")
    fun effectiveOutputKind(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
                           ): OutputKind

    fun effectiveOutputKind(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        canBeAttribute: Boolean
                           ): OutputKind {
        val base = effectiveOutputKind(serializerParent, tagParent)
        if (!canBeAttribute && base == OutputKind.Attribute) {
            return handleAttributeOrderConflict(
                serializerParent,
                tagParent,
                base
                                               )
        }
        return base
    }

    fun handleUnknownContent(
        input: XmlReader,
        inputKind: InputKind,
        name: QName?,
        candidates: Collection<Any>
                            )

    fun handleAttributeOrderConflict(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        outputKind: OutputKind
                                    ): OutputKind {
        throw SerializationException("Node ${serializerParent.elementUseNameInfo.serialName} wants to be an attribute but cannot due to ordering constraints")
    }

    fun shouldEncodeElementDefault(elementDescriptor: XmlDescriptor?): Boolean

    /**
     * Allow modifying the ordering of children.
     */
    fun initialChildReorderMap(
        parentDescriptor: SerialDescriptor
                              ): Collection<XmlOrderConstraint>? = null

    fun updateReorderMap(
        original: Collection<XmlOrderConstraint>,
        children: List<XmlDescriptor>
                        ): Collection<XmlOrderConstraint> = original

    fun enumEncoding(enumDescriptor: SerialDescriptor, index: Int): String {
        return enumDescriptor.getElementName(index)
    }

    enum class XmlEncodeDefault {
        ALWAYS, ANNOTATED, NEVER
    }

}


@OptIn(ExperimentalSerializationApi::class)
open class DefaultXmlSerializationPolicy(
    val pedantic: Boolean,
    val autoPolymorphic: Boolean = false,
    val encodeDefault: XmlEncodeDefault = XmlEncodeDefault.ANNOTATED,
    private val unknownChildHandler: UnknownChildHandler = XmlConfig.DEFAULT_UNKNOWN_CHILD_HANDLER
                                        ) : XmlSerializationPolicy {

    constructor(
        pedantic: Boolean,
        autoPolymorphic: Boolean = false,
        unknownChildHandler: UnknownChildHandler = XmlConfig.DEFAULT_UNKNOWN_CHILD_HANDLER
               ) : this(
        pedantic,
        autoPolymorphic,
        XmlEncodeDefault.ANNOTATED,
        unknownChildHandler
                       )

    override fun isListEluded(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
                             ): Boolean {
        val useAnnotations = tagParent.elementUseAnnotations
        val isMixed = useAnnotations.firstOrNull<XmlValue>()?.value == true
        if (isMixed) return true

        val reqChildrenName =
            useAnnotations.firstOrNull<XmlChildrenName>()?.toQName()
        return reqChildrenName == null // TODO use the policy
    }

    override fun isTransparentPolymorphic(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
                                         ): Boolean {
        val xmlPolyChildren =
            tagParent.elementUseAnnotations.firstOrNull<XmlPolyChildren>()
        return autoPolymorphic || xmlPolyChildren != null
    }

    override fun effectiveOutputKind(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
                                    ): OutputKind {
        return effectiveOutputKind(serializerParent, tagParent, true)
    }

    override fun effectiveOutputKind(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        canBeAttribute: Boolean
                                    ): OutputKind {
        val serialDescriptor = serializerParent.elementSerialDescriptor

        return when (val overrideOutputKind =
            serializerParent.elementUseOutputKind) {
            null             -> {
                val useAnnotations = tagParent.elementUseAnnotations
                val isValue =
                    useAnnotations.firstOrNull<XmlValue>()?.value == true
                var parentChildDesc = tagParent.elementSerialDescriptor
                while (parentChildDesc.isInline) {
                    parentChildDesc =
                        parentChildDesc.getElementDescriptor(0)
                }
                val elementKind = parentChildDesc.kind
                // If we can't be an attribue
                when {
                    elementKind == StructureKind.CLASS -> OutputKind.Element
                    isValue                            -> OutputKind.Mixed
                    ! canBeAttribute &&
                            (tagParent.elementUseOutputKind == OutputKind.Attribute) ->
                        handleAttributeOrderConflict(serializerParent, tagParent, OutputKind.Attribute)

                    ! canBeAttribute -> OutputKind.Element

                    else                               -> tagParent.elementUseOutputKind
                        ?: serialDescriptor.declOutputKind()
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


    override fun serialNameToQName(
        serialName: String,
        parentNamespace: Namespace
                                  ): QName {
        return serialName.substringAfterLast('.').toQname(parentNamespace)
    }

    @OptIn(XmlUtilInternal::class)
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
                                               -> serialUseNameToQName(
                useName,
                parentNamespace
                                                                      )

            typeNameInfo.annotatedName != null -> typeNameInfo.annotatedName

            else                               -> serialTypeNameToQName(
                typeNameInfo,
                parentNamespace
                                                                       )
        }
    }

    override fun shouldEncodeElementDefault(elementDescriptor: XmlDescriptor?): Boolean {
        return when (encodeDefault) {
            XmlEncodeDefault.NEVER     -> false
            XmlEncodeDefault.ALWAYS    -> true
            XmlEncodeDefault.ANNOTATED -> (elementDescriptor as? XmlValueDescriptor)?.default == null
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

    /**
     * Default implementation that uses [XmlBefore] and [XmlAfter]. It does
     * not use the parent descriptor at all.
     */
    override fun initialChildReorderMap(
        parentDescriptor: SerialDescriptor
                                       ): Collection<XmlOrderConstraint>? {
        val nameToIdx =
            (0 until parentDescriptor.elementsCount).associateBy {
                parentDescriptor.getElementName(it)
            }

        fun String.toChildIndex(): Int {
            return nameToIdx[this]
                ?: throw XmlSerialException("Could not find the attribute with the name: $this\n  Candidates were: ${nameToIdx.keys.joinToString()}")
        }

        val orderConstraints = HashSet<XmlOrderConstraint>()
        val orderNodes = mutableMapOf<String, XmlOrderNode>()
        for (elementIdx in 0 until parentDescriptor.elementsCount) {
            var xmlBefore: Array<out String>? = null
            var xmlAfter: Array<out String>? = null
            for (annotation in parentDescriptor.getElementAnnotations(elementIdx)) {
                if (annotation is XmlBefore && annotation.value.isNotEmpty()) {
                    annotation.value.mapTo(orderConstraints) {
                        val successorIdx = it.toChildIndex()
                        XmlOrderConstraint(elementIdx, successorIdx)
                    }
                    xmlBefore = annotation.value
                } else if (annotation is XmlAfter && annotation.value.isNotEmpty()) {
                    annotation.value.mapTo(orderConstraints) {
                        val predecessorIdx = it.toChildIndex()
                        XmlOrderConstraint(predecessorIdx, elementIdx)
                    }
                    xmlAfter = annotation.value
                }
                if (xmlBefore != null || xmlAfter != null) {
                    val node = orderNodes.getOrPut(
                        parentDescriptor.getElementName(elementIdx)
                                                  ) {
                        XmlOrderNode(
                            elementIdx
                                    )
                    }
                    if (xmlBefore != null) {
                        val befores = Array(xmlBefore.size) {
                            val name = xmlBefore[it]
                            orderNodes.getOrPut(name) { XmlOrderNode(name.toChildIndex()) }
                        }
                        node.addSuccessors(*befores)
                    }
                    if (xmlAfter != null) {
                        val afters = Array(xmlAfter.size) {
                            val name = xmlAfter[it]
                            orderNodes.getOrPut(name) { XmlOrderNode(name.toChildIndex()) }
                        }
                        node.addPredecessors(*afters)
                    }

                }
            }
        }
        if (orderNodes.isEmpty()) return null // no order nodes, no reordering

        return if (orderConstraints.isEmpty()) null else orderConstraints.toList()
    }

    override fun updateReorderMap(
        original: Collection<XmlOrderConstraint>,
        children: List<XmlDescriptor>
                                 ): Collection<XmlOrderConstraint> {

        fun Int.isAttribute(): Boolean = children[this].outputKind == OutputKind.Attribute

        return original.filter { constraint ->
            val (isBeforeAttribute, isAfterAttribute) = constraint.map(Int::isAttribute)

            isBeforeAttribute || (!isAfterAttribute)
        }
    }

    override fun ignoredSerialInfo(message: String) {
        if (pedantic) throw XmlSerialException(message)
    }
}
