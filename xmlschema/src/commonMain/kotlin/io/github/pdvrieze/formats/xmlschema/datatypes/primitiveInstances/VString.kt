/*
 * Copyright (c) 2021.
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

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.isNCName
import io.github.pdvrieze.formats.xmlschema.types.isContentEqual
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import nl.adaptivity.xmlutil.NamespaceContext
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.xmlCollapseWhitespace
import kotlin.jvm.JvmInline

@Serializable(VString.Serializer::class)
interface VString : VAnyAtomicType, CharSequence {
    override val length: Int get() = xmlString.length

    override fun get(index: Int): Char = xmlString[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = xmlString.subSequence(startIndex, endIndex)
    fun toLong(): Long = xmlCollapseWhitespace(xmlString).toLong()
    fun toInt(): Int = xmlCollapseWhitespace(xmlString).toInt()
    fun toULong(): ULong = xmlCollapseWhitespace(xmlString).toULong()
    fun toUInt(): UInt = xmlCollapseWhitespace(xmlString).toUInt()
    fun toDouble(): Double = xmlCollapseWhitespace(xmlString).toDouble()
    fun toFloat(): Float = xmlCollapseWhitespace(xmlString).toFloat()

    @JvmInline
    private value class Inst(override val xmlString: String) : VString {
/*
        override fun equals(other: Any?): Boolean = when (other) {
            is VPrefixString -> false
            is VString -> xmlString == other.xmlString
            else -> false
        }
*/

        override fun toString(): String {
            return xmlString
        }
    }

    @OptIn(XmlUtilInternal::class)
    class Serializer : SimpleTypeSerializer<VString>("io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString") {

        private fun toVString(namespaceContext: NamespaceContext, noSpaceStr: String, cPos: Int = noSpaceStr.indexOf(':')): VPrefixString? {
            if (cPos > 0) {
                if (noSpaceStr.indexOf(':', cPos + 1) < 0) {

                    val prefix = noSpaceStr.substring(0, cPos)
                    val ns = namespaceContext.getNamespaceURI(prefix)
                    if (ns != null) {
                        val localName = noSpaceStr.substring(cPos + 1)
                        if (prefix.isNCName() && localName.isNCName()) {
                            return VPrefixString(ns, prefix, localName)
                        }
                    }
                }
            } else {
                val defaultNamespace = namespaceContext.getNamespaceURI("")
                if ((!defaultNamespace.isNullOrEmpty()) && noSpaceStr.isNCName()) {
                    return VPrefixString(defaultNamespace, "", noSpaceStr)
                }
            }
            return null
        }

        override fun deserialize(decoder: Decoder): VString {
            val strRepr = decoder.decodeString()
            if (decoder is XML.XmlInput) {
                var cpos = -1
                var hasSpc = false
                var seenNonSpace =false
                for (i in strRepr.indices) {
                    when(strRepr[i]) {
                        ' ' -> if(seenNonSpace) { hasSpc = true; if (cpos>=0) break }
                        ':' -> { cpos = i; seenNonSpace = true; if (hasSpc) break }
                        else -> seenNonSpace = true
                    }
                }

                if (hasSpc) {
                    var hasPrefix = false
                    val elems = buildList {
                        for(s in strRepr.split(' ')) {
                            when(val n = toVString(decoder.input.namespaceContext, s)) {
                                null -> add(VString(s))
                                else -> {
                                    hasPrefix = true
                                    add(n)
                                }
                            }

                        }
                    }
                    if (hasPrefix) return VPrefixStringList(elems) else return VString(strRepr)
                } else {
                    return toVString(decoder.input.namespaceContext, strRepr, cpos) ?: VString(strRepr)
                }
            }
            return Inst(strRepr)
        }
    }

    companion object {
        operator fun invoke(value: String): VString = Inst(value)
    }
}

/**
 * Special string type that captures a namespace
 */
class VPrefixString(val namespace: String, val prefix: String, val localname: String) : VString {
    override val xmlString: String
        get() = when {
            prefix.isEmpty() -> localname
            else -> "$prefix:$localname"
        }

    fun toQName(): QName = QName(namespace, localname, prefix)

    fun toVQName(): VQName = VQName(namespace, localname, prefix)

    override fun toString(): String = xmlString
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        when (other) {
            is VPrefixString -> {
                if (namespace != other.namespace) return false
                if (prefix != other.prefix) return false
                if (localname != other.localname) return false

                return true
            }
            is VString -> {
                return xmlString == other.xmlString
            }
            else -> return false
        }
    }

    override fun hashCode(): Int = xmlString.hashCode()


}

/**
 * Special string type that captures a namespace
 */
class VPrefixStringList(val elems: List<VString>) : VString {
    override val xmlString: String
        get() = elems.joinToString(" ")

    fun toQNames(): List<QName> = elems.mapNotNull {
        when {
            it is VPrefixString -> it.toQName()
            it.isEmpty() -> null
            else -> QName(it.xmlString)
        }
    }

    fun toVQNames(): List<VQName> = elems.mapNotNull {
        when {
            it is VPrefixString -> it.toVQName()
            it.isEmpty() -> null
            else -> VQName(it.xmlString)
        }
    }

    override fun toString(): String = xmlString
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        elems.singleOrNull()?.let { return it == other }

        return when (other) {
            is VPrefixStringList -> elems.isContentEqual(other.elems)
            is VPrefixString -> elems.singleOrNull()?.let { it == other } ?: false
            is VString -> xmlString == other.xmlString

            else -> false
        }
    }

    override fun hashCode(): Int = xmlString.hashCode()


}
