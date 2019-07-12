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

import kotlinx.serialization.PrimitiveKind
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.StructureKind
import kotlinx.serialization.UnionKind
import kotlinx.serialization.modules.SerialModule
import nl.adaptivity.xmlutil.*
import kotlin.jvm.JvmStatic

internal open class XmlCodecBase internal constructor(
    val context: SerialModule,
    val config: XmlConfig
                                                     ) {

    companion object {

        @JvmStatic
        protected fun SerialDescriptor.requestedChildName(index: Int): QName? {
            return getElementAnnotations(index).getChildName()

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
                    childDesc?.getEntityAnnotations()?.firstOrNull<XmlSerialName>()?.let { return it.toQName() }
                    return getElementName(index).toQname()
                }
                OutputKind.Text      -> return getElementName(index).toQname(parentNamespace) // Will be ignored anyway
                else                 -> { // Not an attribute, will take name from type
                    if (elementsCount > 0) {
                        childDesc?.getEntityAnnotations()?.firstOrNull<XmlSerialName>()?.let { return it.toQName() }
                        // elementDesc.name is the type for classes, but not for "special kinds" as those have generic names
                        return when (childDesc?.kind) {
                            StructureKind.CLASS -> childDesc.name.substringAfterLast('.').toQname(parentNamespace)
                            else                -> getElementName(index).toQname(parentNamespace)
                        }
                    } else if (index == 0) { // We are in a list or something that has a confused descriptor
                        return getElementName(0).toQname(parentNamespace)
                    } else { // index >0
                        if (childDesc == null || childDesc.kind is PrimitiveKind) {
                            return getElementName(index).toQname(parentNamespace)
                        } else {
                            childDesc.getEntityAnnotations().firstOrNull<XmlSerialName>()?.let { return it.toQName() }
                            // elementDesc.name is normally the type name. We don't want dotted names anyway so strip those
                            return childDesc.name.substringAfterLast('.').toQname(parentNamespace)
                        }
                    }
                }
            }
        }

        @JvmStatic
        internal fun SerialDescriptor.outputKind(index: Int, childDesc: SerialDescriptor?): OutputKind {
            if (index < 0) {
                return OutputKind.Element
            }
            // The children of these are always elements
            when (kind) {
                StructureKind.LIST,
                StructureKind.MAP,
                UnionKind.POLYMORPHIC,
                UnionKind.SEALED -> return OutputKind.Element
            }
            if (index < elementsCount) {// This can be false for lists, they are always elements anyway
                for (annotation in getElementAnnotations(index)) {
                    when (annotation) {
                        is XmlChildrenName -> return OutputKind.Element
                        is XmlElement      -> return if (annotation.value) OutputKind.Element else OutputKind.Attribute
                        is XmlValue        -> if (annotation.value) return OutputKind.Text
                    }
                }
            }

            // Lists are always elements
            if (childDesc != null) {
                if (childDesc.elementsCount > 1) return OutputKind.Element
                childDesc.getEntityAnnotations().firstOrNull<XmlElement>()
                    ?.let { if (it.value) return OutputKind.Element else OutputKind.Attribute }
            }

            return when (childDesc?.kind) {
                null,
                is PrimitiveKind -> OutputKind.Attribute
                else             -> OutputKind.Element
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

    abstract inner class XmlCodec(
        val parentNamespace: Namespace,
        val parentDesc: SerialDescriptor,
        val elementIndex: Int,
        protected val childDesc: SerialDescriptor?
                                 ) {
        open val serialName: QName
            get() = parentDesc.requestedName(parentNamespace, elementIndex, childDesc)
    }

    internal abstract inner class XmlTagCodec(
        val parentDesc: SerialDescriptor,
        val elementIndex: Int,
        val desc: SerialDescriptor,
        val parentNamespace: Namespace
                                             ) {
        val context: SerialModule get() = this@XmlCodecBase.context

        open val serialName: QName get() = parentDesc.requestedName(parentNamespace, elementIndex, desc)

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

        /**
         * Determine the polymorphic tag name for a particular element.
         */
        fun polyTagName(
            parentTag: QName,
            polyChild: String,
            itemIdx: Int
                       ): PolyInfo {
            val currentTypeName = parentDesc.name
            val currentPkg = currentTypeName.substringBeforeLast('.', "")
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

            val typename = if (pkgPos >= 0 || currentPkg.isEmpty()) typeNameBase else "$currentPkg.$typeNameBase"

            val name = QName(ns, localPart, prefix)

            return PolyInfo(typename, name, itemIdx)
        }

        /**
         * Given a parent tag, record all polymorphic children.
         */
        fun polyInfo(
            parentTag: QName,
            polyChildren: Array<String>
                    ): XmlNameMap {
            val result = XmlNameMap()

            for (polyChild in polyChildren) {
                val polyInfo = polyTagName(parentTag, polyChild, -1)

                result.registerClass(polyInfo.tagName, polyInfo.kClass, polyChild.indexOf('=') >= 0)
            }

            return result
        }

    }

}
