/*
 * Copyright (c) 2022.
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

package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.toCName
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.XmlUtilInternal
import org.w3c.dom.Document
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Attr
import org.w3c.dom.Element
import org.w3c.dom.Node


internal fun Document.createElement(name: QName): Element {
    return createElementNS(name.getNamespaceURI(), name.toCName())
}

internal val Node.isElement: Boolean get() = nodeType == Node.ELEMENT_NODE

internal val Node.isText: Boolean
    get() = when (nodeType) {
        Node.ELEMENT_NODE, Node.CDATA_SECTION_NODE -> true
        else -> false
    }

/** Implement forEach with a built-in cast to attr. */
internal inline fun NamedNodeMap.forEachAttr(body: (Attr) -> Unit) {
    val l = this.length
    for (idx in 0 until l) {
        body(item(idx) as Attr)
    }
}

/** Remove all the child nodes that are elements. */
internal fun Node.removeElementChildren() {
    val top = this
    var cur = top.firstChild
    while (cur != null) {
        val n = cur.nextSibling
        if (cur.isElement) {
            top.removeChild(cur)
        }
        cur = n
    }
}

/** A filter function on a [NamedNodeMap] that returns a list of all
 * (attributes)[Attr] that meet the [predicate].
 */
internal inline fun <reified T : Node> NamedNodeMap.filterTyped(predicate: (T) -> Boolean): List<T> {
    val result = mutableListOf<T>()
    forEachAttr { attr ->
        if (attr is T && predicate(attr)) result.add(attr)
    }
    return result
}

internal fun Node.myLookupPrefix(namespaceUri: String): String? {
    if (this !is Element) return null
    attributes.forEachAttr { attr ->
        when {
            attr.prefix == XMLConstants.XMLNS_ATTRIBUTE &&
                    attr.value == namespaceUri
            -> return attr.localName

            attr.prefix.isNullOrBlank() && attr.localName == XMLConstants.XMLNS_ATTRIBUTE &&
                    attr.value == namespaceUri
            -> return ""
        }
    }
    return parentNode?.myLookupPrefix(namespaceUri)
}

internal fun Node.myLookupNamespaceURI(prefix: String): String? = when (this) {
    !is Element -> null
    else -> {
        attributes.filterTyped<Attr> {
            (prefix == "" && it.localName == "xmlns") ||
                    (it.prefix == "xmlns" && it.localName == prefix)
        }.firstOrNull()?.value ?: parentNode?.myLookupNamespaceURI(prefix)
    }
}
