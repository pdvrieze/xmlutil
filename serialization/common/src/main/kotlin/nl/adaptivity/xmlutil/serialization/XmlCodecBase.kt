/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.SerialContext
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.compat.PrimitiveKind
import nl.adaptivity.xmlutil.serialization.compat.SerialDescriptor
import nl.adaptivity.xmlutil.serialization.compat.StructureKind
import nl.adaptivity.xmlutil.serialization.compat.UnionKind
import kotlin.jvm.JvmStatic

internal open class XmlCodecBase internal constructor(val context: SerialContext?) {

    companion object {

        @JvmStatic
        protected fun SerialDescriptor.requestedChildName(index: Int): QName? {
            return getElementAnnotations(index).getChildName()

        }

        @JvmStatic
        protected fun SerialDescriptor.requestedName(index: Int): QName {
            getElementAnnotations(index).firstOrNull<XmlSerialName>()?.run { return toQName() }
            when (outputKind(index)) {
                OutputKind.Attribute -> { // Attribute will take name from use
                    val elementDesc = getElementDescriptor(index)
                    elementDesc.getEntityAnnotations().firstOrNull<XmlSerialName>()?.let { return it.toQName() }
                    return getElementName(index).toQname()
                }
                OutputKind.Text      -> return getElementName(index).toQname() // Will be ignored anyway
                else                 -> { // Not an attribute, will take name from type
                    if (elementsCount > 0) {
                        val elementDesc = getElementDescriptor(index)
                        elementDesc.getEntityAnnotations().firstOrNull<XmlSerialName>()?.let { return it.toQName() }
                        // elementDesc.name is the type for classes, but not for "special kinds" as those have generic names
                        return when (elementDesc.extKind) {
                            StructureKind.CLASS -> elementDesc.name.substringAfterLast('.').toQname()
                            else                -> getElementName(index).toQname()
                        }
                    } else if (index == 0) { // We are in a list or something that has a confused descriptor
                        return QName(getElementName(0))
                    } else { // index >0
                        val elementDesc = getElementDescriptor(1)
                        if (elementDesc.extKind is PrimitiveKind) {
                            return getElementName(index).toQname()
                        } else {
                            elementDesc.getEntityAnnotations().firstOrNull<XmlSerialName>()?.let { return it.toQName() }
                            // elementDesc.name is normally the type name. We don't want dotted names anyway so strip those
                            return elementDesc.name.substringAfterLast('.').toQname()
                        }
                    }
                }
            }
        }

        @JvmStatic
        internal fun SerialDescriptor.outputKind(index: Int): OutputKind {
            if (index < 0) {
                return OutputKind.Element
            }
            // The children of these are always elements
            when (extKind) {
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
            val elementDesc = getElementDescriptor(index.coerceAtMost(elementsCount - 1))
            if (elementDesc.elementsCount > 1) return OutputKind.Element
            elementDesc.getEntityAnnotations().firstOrNull<XmlElement>()?.let { if (it.value) return OutputKind.Element else OutputKind.Attribute }

            return when (elementDesc.extKind) {
                is PrimitiveKind -> OutputKind.Attribute
                else             -> OutputKind.Element
            }
        }
    }

    abstract inner class XmlCodec(val parentDesc: SerialDescriptor, val elementIndex: Int) {
        open val serialName: QName
            get() = parentDesc.requestedName(elementIndex)
    }

    internal abstract inner class XmlTagCodec(val parentDesc: SerialDescriptor,
                                              val elementIndex: Int,
                                              val desc: SerialDescriptor) {
        val context: SerialContext? get() = this@XmlCodecBase.context

        open val serialName: QName get() = parentDesc.requestedName(elementIndex)

        abstract val namespaceContext: NamespaceContext

        internal fun QName.normalize(): QName {
            return when {
                namespaceURI.isEmpty() -> copy(namespaceURI = namespaceContext.getNamespaceURI(prefix) ?: "",
                                               prefix = "")
                else                   -> copy(prefix = "")
            }
        }


        fun polyTagName(parentTag: QName,
                        polyChild: String,
                        itemIdx: Int): PolyInfo {
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

        fun polyInfo(parentTag: QName,
                     polyChildren: Array<String>): XmlNameMap {
            val result = XmlNameMap()

            for (polyChild in polyChildren) {
                val polyInfo = polyTagName(parentTag, polyChild, -1)

                result.registerClass(polyInfo.tagName, polyInfo.kClass, polyChild.indexOf('=') >= 0)
            }

            return result
        }

    }

}