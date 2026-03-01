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

package io.github.pdvrieze.xml.schematypes.values.instances

import io.github.pdvrieze.xml.schematypes.values.VQName
import io.github.pdvrieze.xml.schematypes.values.XSString
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.QName

/**
 * Special string type that captures a namespace
 */
@ExperimentalXmlUtilApi
class XSPrefixStringList(val elems: List<XSString>) :
    XSString {
    override val xmlString: String
        get() = elems.joinToString(" ")

    fun toQNames(): List<QName> = elems.mapNotNull {
        when {
            it is XSPrefixString -> it.toQName()
            it.isEmpty() -> null
            else -> QName(it.xmlString.toString())
        }
    }

    fun toVQNames(): List<VQName> = elems.mapNotNull {
        when {
            it is XSPrefixString -> it.toVQName()
            it.isEmpty() -> null
            else -> VQName(it.xmlString.toString())
        }
    }

    override fun toString(): String = xmlString
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        elems.singleOrNull()?.let { return it == other }

        return when (other) {
            is XSPrefixStringList -> isContentEqual(other.elems)
            is XSPrefixString -> elems.singleOrNull()?.let { it == other } ?: false
            is XSString -> xmlString == other.xmlString

            else -> false
        }
    }

    private fun isContentEqual(other: List<XSString>): Boolean {
        if (elems.size != other.size) return false
        return elems.zip(other).all { (a, b) -> a == b }
    }

    override fun hashCode(): Int = xmlString.hashCode()


}
