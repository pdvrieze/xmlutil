/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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
import kotlinx.serialization.modules.SerialModule
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.impl.getPolymorphic
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass

internal abstract class XmlCodecBase internal constructor(
    val context: SerialModule,
    val config: XmlConfig
                                                         ) {

    protected abstract val namespaceContext: NamespaceContext

    /**
     * Determine the polymorphic tag name for a particular element.
     */
    fun polyTagName(
        parentDesc: SerialDescriptor,
        parentTag: QName,
        polyChild: String,
        itemIdx: Int,
        baseClass: KClass<*>
                   ): PolyInfo {
        return polyTagNameCommon(parentDesc, parentTag, polyChild, itemIdx) { childName ->
            getPolymorphic(baseClass, childName)
        }
    }

    /**
     * Determine the polymorphic tag name for a particular element.
     */
    fun polyTagName(
        parent: XmlSerializationPolicy.NameInfo,
        polyChild: String,
        itemIdx: Int,
        baseClass: KClass<*>
                   ): PolyInfo {
        return polyTagNameCommon(parent, polyChild, itemIdx) { childName ->
            getPolymorphic(baseClass, childName)
        }
    }

    /**
     * Determine the polymorphic tag name for a particular element.
     */
    fun polyTagName(
        parentDesc: SerialDescriptor,
        parentTag: QName,
        polyChild: String,
        itemIdx: Int,
        baseClassName: String
                   ): PolyInfo {
        return polyTagNameCommon(parentDesc, parentTag, polyChild, itemIdx) { childName ->
            getPolymorphic(baseClassName, childName)
        }
    }

    private fun polyTagNameCommon(
        parentDesc: SerialDescriptor,
        parentTag: QName,
        polyChild: String,
        itemIdx: Int,
        getPolymorphic: SerialModule.(String) -> KSerializer<*>?
                                 ): PolyInfo {
        return polyTagNameCommon(
            XmlSerializationPolicy.NameInfo(parentDesc.serialName, parentTag),
            polyChild,
            itemIdx,
            getPolymorphic
                                )
    }

    private fun polyTagNameCommon(
        parent: XmlSerializationPolicy.NameInfo,
        polyChild: String,
        itemIdx: Int,
        getPolymorphic: SerialModule.(String) -> KSerializer<*>?
                                 ): PolyInfo {
        val currentPkg = parent.serialName.substringBeforeLast('.', "")
        val parentTag = parent.annotatedName!!
        val eqPos = polyChild.indexOf('=')
        val pkgPos: Int
        val prefPos: Int
        val typeNameBase: String
        val prefix: String
        val localPart: String

        if (eqPos < 0) {
            typeNameBase = polyChild
            pkgPos = polyChild.lastIndexOf('.')
            prefPos = -1
            prefix = parentTag.prefix
            localPart = if (pkgPos < 0) polyChild else polyChild.substring(pkgPos + 1)
        } else {
            typeNameBase = polyChild.substring(0, eqPos).trim()
            pkgPos = polyChild.lastIndexOf('.', eqPos - 1)
            prefPos = polyChild.indexOf(':', eqPos + 1)

            if (prefPos < 0) {
                prefix = parentTag.prefix
                localPart = polyChild.substring(eqPos + 1).trim()
            } else {
                prefix = polyChild.substring(eqPos + 1, prefPos).trim()
                localPart = polyChild.substring(prefPos + 1).trim()
            }
        }

        val ns = if (prefPos >= 0) namespaceContext.getNamespaceURI(prefix)
            ?: parentTag.namespaceURI else parentTag.namespaceURI

        val typename = when {
            pkgPos != 0 || currentPkg.isEmpty()
                 -> typeNameBase

            else -> "$currentPkg.${typeNameBase.substring(1)}"
        }

        val descriptor = context.getPolymorphic(typename)?.descriptor
            ?: throw XmlException("Missing descriptor for $typename in the serial context")
        val name: QName = if (eqPos < 0) {
            descriptor
                ?.annotations
                ?.firstOrNull<XmlSerialName>()
                ?.toQName()
                ?: QName(ns, localPart, prefix)
        } else {
            QName(ns, localPart, prefix)
        }

        return PolyInfo(typename, name, itemIdx, descriptor)
    }


    /**
     * Given a parent tag, record all polymorphic children.
     */
    fun polyInfo(
        parentDesc: SerialDescriptor,
        parentTag: QName,
        polyChildren: Array<String>,
        baseClass: KClass<*>
                ): XmlNameMap {
        return polyInfo(XmlSerializationPolicy.NameInfo(parentDesc.serialName, parentTag), polyChildren, baseClass)
    }

    /**
     * Given a parent tag, record all polymorphic children.
     */
    fun polyInfo(
        parent: XmlSerializationPolicy.NameInfo,
        polyChildren: Array<String>,
        baseClass: KClass<*>
                ): XmlNameMap {
        val result = XmlNameMap()

        for (polyChild in polyChildren) {
            val polyInfo = polyTagName(parent, polyChild, -1, baseClass)

            result.registerClass(polyInfo.tagName, polyInfo.describedName, polyChild.indexOf('=') >= 0)
        }

        return result
    }


    companion object {

        @JvmStatic
        protected fun SerialDescriptor.requestedChildName(index: Int): QName? {
            return getElementAnnotations(index).firstOrNull<XmlChildrenName>()?.toQName()

        }

        @JvmStatic
        protected fun SerialDescriptor.requestedName(
            parentNamespace: Namespace,
            index: Int,
            childDesc: SerialDescriptor?
                                                    ): QName {
            getElementAnnotations(index).firstOrNull<XmlSerialName>()?.run { return toQName() }
            when (outputKind(index, childDesc)) {
                OutputKind.Attribute -> { // Attribute will take name from use
                    childDesc?.annotations?.firstOrNull<XmlSerialName>()?.let { return it.toQName() }
                    return getElementName(index).toQname()
                }
                OutputKind.Text      -> return getElementName(index).toQname(parentNamespace) // Will be ignored anyway
                else                 -> { // Not an attribute, will take name from type (mixed will be the same)
                    if (elementsCount > 0) {
                        childDesc?.annotations?.firstOrNull<XmlSerialName>()?.let { return it.toQName() }
                        // elementDesc.name is the type for classes, but not for "special kinds" as those have generic names
                        return when (childDesc?.kind) {
                            StructureKind.CLASS -> childDesc.serialName.substringAfterLast('.').toQname(parentNamespace)
                            else                -> getElementName(index).toQname(parentNamespace)
                        }
                    } else if (index == 0) { // We are in a list or something that has a confused descriptor
                        return getElementName(0).toQname(parentNamespace)
                    } else { // index >0
                        if (childDesc == null || childDesc.kind is PrimitiveKind) {
                            return getElementName(index).toQname(parentNamespace)
                        } else {
                            childDesc.annotations.firstOrNull<XmlSerialName>()?.let { return it.toQName() }
                            // elementDesc.name is normally the type name. We don't want dotted names anyway so strip those
                            return childDesc.serialName.substringAfterLast('.').toQname(parentNamespace)
                        }
                    }
                }
            }
        }

        internal fun SerialDescriptor.declRequestedName(parentNamespace: Namespace): QName {
            annotations.firstOrNull<XmlSerialName>()?.let { return it.toQName() }
            return serialName.substringAfterLast('.').toQname(parentNamespace)
        }

        @JvmStatic
        internal fun SerialDescriptor.outputKind(index: Int, childDesc: SerialDescriptor?): OutputKind {
            if (index < 0) {
                return OutputKind.Element
            }

            val valueChildIndex = this.getValueChild()

            fun OutputKind.checkValueChild(): OutputKind = also {
                if (valueChildIndex >= 0 && index != valueChildIndex && it == OutputKind.Element) {
                    throw XmlSerialException("Types with an @XmlValue member may not contain other child elements")
                }
            }

            // The children of these are always elements

            when (kind) {
                StructureKind.LIST,
                StructureKind.MAP,
                is PolymorphicKind -> return OutputKind.Element.checkValueChild()
            }


            if (index == valueChildIndex) {
                return when (childDesc?.kind) {
                    null,
                    is PrimitiveKind      -> OutputKind.Text
                    is PolymorphicKind,
                    is StructureKind.LIST -> OutputKind.Mixed
                    else                  -> OutputKind.Element//throw XmlSerialException("@XmlValue annotations can only be put on primitive and list types, not ${childDesc?.kind}")
                }
            } else if (index < elementsCount) {// This can be false for lists, they are always elements anyway
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
        }

        internal fun String.expandTypeNameIfNeeded(parentType: String): String {
            if (!startsWith('.')) return this
            val parentPkg = parentType.lastIndexOf('.').let { idx ->
                if (idx < 0) return substring(1)
                parentType.substring(0, idx)
            }
            return "$parentPkg$this"
        }

        internal fun String.tryShortenTypeName(parentType: String): String {
            val parentPkg = parentType.lastIndexOf('.').let { idx ->
                if (idx < 0) return this
                parentType.substring(0, idx)
            }
            if (startsWith(parentPkg) && indexOf('.', parentPkg.length + 1) < 0) {
                return substring(parentPkg.length) // include starting . to signal relative type
            }
            return this
        }
    }

    abstract inner class XmlCodec<out D : XmlDescriptor>(
        protected val xmlDescriptor: D,
        protected val parentDesc: SerialDescriptor,
        protected val elementIndex: Int,
        protected val childDesc: SerialDescriptor?
                                                        ) {

        val serialName: QName get() = xmlDescriptor.tagName
    }

    internal abstract inner class XmlTagCodec<out D : XmlDescriptor>(
        val parentDesc: SerialDescriptor,
        val elementIndex: Int,
        val desc: SerialDescriptor,
        val xmlDescriptor: D
                                                                    ) {
        internal val config get() = this@XmlCodecBase.config
        val context: SerialModule get() = this@XmlCodecBase.context

        val serialName: QName get() = xmlDescriptor.tagName

        abstract val namespaceContext: NamespaceContext

        internal fun QName.normalize(): QName {
            return when {
                namespaceURI.isEmpty() -> copy(
                    namespaceURI = namespaceContext.getNamespaceURI(prefix) ?: "",
                    prefix = ""
                                              )
                else                   -> copy(prefix = "")
            }
        }

    }

}
