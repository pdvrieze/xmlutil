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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.jvm.JvmInline

@Serializable(VString.Serializer::class)
interface VString : VAnyAtomicType, CharSequence {
    override val length: Int get() = xmlString.length

    override fun get(index: Int): Char = xmlString[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = xmlString.subSequence(startIndex, endIndex)
    fun toLong(): Long = xmlString.toLong()
    fun toInt(): Int = xmlString.toInt()
    fun toULong(): ULong = xmlString.toULong()
    fun toUInt(): UInt = xmlString.toUInt()
    fun toDouble(): Double = xmlString.toDouble()
    fun toFloat(): Float = xmlString.toFloat()

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
        override fun deserialize(decoder: Decoder): VString {
            val strRepr = decoder.decodeString()
            if (decoder is XML.XmlInput) {
                val cpos = strRepr.indexOf(':')

                if (cpos > 0) {
                    if (strRepr.indexOf(':', cpos + 1) < 0) {

                        val prefix = strRepr.substring(0, cpos)
                        val ns = decoder.input.namespaceContext.getNamespaceURI(prefix)
                        if (ns != null) {
                            val localName = strRepr.substring(cpos + 1)
                            if (prefix.isNCName() && localName.isNCName()) {
                                return VPrefixString(ns, prefix, localName)
                            }
                        }
                    }
                } else {
                    val defaultNamespace = decoder.input.namespaceContext.getNamespaceURI("")
                    if ((!defaultNamespace.isNullOrEmpty()) && strRepr.isNCName()) {
                        return VPrefixString(defaultNamespace, "", strRepr)
                    }
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

    override fun toString(): String = xmlString
}
