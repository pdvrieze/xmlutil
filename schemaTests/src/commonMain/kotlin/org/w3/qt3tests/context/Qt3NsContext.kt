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

package org.w3.qt3tests.context

import io.github.pdvrieze.formats.xpath.functions.BuiltinFunction
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import nl.adaptivity.xmlutil.IterableNamespaceContext
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.XmlEvent

@OptIn(XPathInternal::class)
object Qt3NsContext : IterableNamespaceContext {
    private val base = mapOf(
        "xml" to XMLConstants.XML_NS_URI,
        "xmlns" to XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
        "xs" to XMLConstants.XSD_NS_URI,
        "fn" to BuiltinFunction.FN_NAMESPACE,
        "map" to BuiltinFunction.MAP_NAMESPACE,
        "array" to BuiltinFunction.ARRAY_NAMESPACE,
        "math" to BuiltinFunction.MATH_NAMESPACE,
        "ERR" to "http://www.w3.org/2005/xqt-errors",
    )

    private val reverse by lazy { base.entries.associate { it.value to it.key } }

    override fun getNamespaceURI(prefix: String): String? {
        return base[prefix]
    }

    override fun getPrefix(namespaceURI: String): String? {
        return reverse[namespaceURI]
    }

    override fun getPrefixes(namespaceURI: String): Iterator<String> {
        return listOfNotNull(getPrefix(namespaceURI)).iterator()
    }

    override fun iterator(): Iterator<Namespace> {
        return base.map { XmlEvent.NamespaceImpl(it.key, it.value) }.iterator()
    }
}
