/*
 * Copyright (c) 2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.github.pdvrieze.xml.schematypes.values

import io.github.pdvrieze.xml.schematypes.impl.SimpleTypeSerializer
import io.github.pdvrieze.xml.schematypes.isNCName
import io.github.pdvrieze.xml.schematypes.types.StringType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdPrefixString
import io.github.pdvrieze.xml.schematypes.values.instances.XsdPrefixStringList
import io.github.pdvrieze.xml.schematypes.values.instances.XsdStringImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.*

@ExperimentalXmlUtilApi
@Serializable(XsdString.Companion::class)
interface XsdString : XsdAtomic, CharSequence {

    override val schemaType: StringType<XsdString>

    override val length: Int get() = xmlString.length

    override fun get(index: Int): Char = xmlString[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = xmlString.subSequence(startIndex, endIndex)
    fun toLong(): Long = xmlCollapseWhitespace(xmlString).toLong()
    fun toInt(): Int = xmlCollapseWhitespace(xmlString).toInt()
    fun toULong(): ULong = xmlCollapseWhitespace(xmlString).toULong()
    fun toUInt(): UInt = xmlCollapseWhitespace(xmlString).toUInt()
    fun toDouble(): Double = xmlCollapseWhitespace(xmlString).toDouble()
    fun toFloat(): Float = xmlCollapseWhitespace(xmlString).toFloat()

    @OptIn(XmlUtilInternal::class)
    companion object : SimpleTypeSerializer<XsdString>("xsd.string") {

        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): XsdString {
            if (input != null) {
                var cpos = -1
                var hasSpc = false
                var seenNonSpace =false
                for (i in raw.indices) {
                    when(raw[i]) {
                        ' ' -> if(seenNonSpace) { hasSpc = true; if (cpos>=0) break }
                        ':' -> { cpos = i; seenNonSpace = true; if (hasSpc) break }
                        else -> seenNonSpace = true
                    }
                }

                if (hasSpc) {
                    var hasPrefix = false
                    val elems = buildList {
                        for(s in raw.split(' ')) {
                            when(val n = toVString(input.namespaceContext, s)) {
                                null -> add(XsdStringImpl(s))
                                else -> {
                                    hasPrefix = true
                                    add(n)
                                }
                            }

                        }
                    }
                    return if (hasPrefix) XsdPrefixStringList(elems) else XsdStringImpl(raw)
                } else {
                    return toVString(input.namespaceContext, raw, cpos) ?: XsdStringImpl(raw)
                }
            }
            return XsdStringImpl(raw)
        }

        private fun toVString(namespaceContext: NamespaceContext, noSpaceStr: String, cPos: Int = noSpaceStr.indexOf(':')): XsdPrefixString? {
            if (cPos > 0) {
                if (noSpaceStr.indexOf(':', cPos + 1) < 0) {

                    val prefix = noSpaceStr.substring(0, cPos)
                    val ns = namespaceContext.getNamespaceURI(prefix)
                    if (ns != null) {
                        val localName = noSpaceStr.substring(cPos + 1)
                        if (prefix.isNCName() && localName.isNCName()) {
                            return XsdPrefixString(
                                ns,
                                prefix,
                                localName
                            )
                        }
                    }
                }
            } else {
                val defaultNamespace = namespaceContext.getNamespaceURI("")
                if ((!defaultNamespace.isNullOrEmpty()) && noSpaceStr.isNCName()) {
                    return XsdPrefixString(
                        defaultNamespace,
                        "",
                        noSpaceStr
                    )
                }
            }
            return null
        }

        operator fun invoke(value: String): XsdString = XsdStringImpl(value)
    }
}
