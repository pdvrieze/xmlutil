/*
 * Copyright (c) 2024-2025.
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

@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.dom2.NamedNodeMap
import nl.adaptivity.xmlutil.dom2.Node


/** Implement forEach with a built-in cast to attr. */
internal inline fun <T: Node> NamedNodeMap<T>.forEachAttr(body: (T) -> Unit) {
    val l = this.getLength()
    for (idx in 0 until l) {
        body(item(idx)!!)
    }
}

internal fun Node.myLookupPrefix(namespaceUri: String): String? {
    return (this as? Element)?.myLookupPrefixImpl(namespaceUri, mutableSetOf())
}

private fun Element.myLookupPrefixImpl(namespaceUri: String, seenPrefixes: MutableSet<String>): String? {
    getAttributes().forEachAttr { attr ->
        when {
            attr.getPrefix() == XMLConstants.XMLNS_ATTRIBUTE ->
                if (attr.getValue() == namespaceUri && attr.getLocalName() !in seenPrefixes) {
                    return attr.getLocalName()
                } else {
                    seenPrefixes.add(attr.getLocalName() ?: attr.getName())
                }

            attr.getPrefix().isNullOrBlank() && attr.getLocalName() == XMLConstants.XMLNS_ATTRIBUTE ->
                if (attr.getValue() == namespaceUri && attr.getLocalName() !in seenPrefixes) {
                    return ""
                } else {
                    seenPrefixes.add("")
                }
        }
    }
    return (getParentNode() as? Element)?.myLookupPrefixImpl(namespaceUri, seenPrefixes)
}

internal fun Node.myLookupNamespaceURI(prefix: String): String? = when (this) {
    is Element -> {
        getAttributes().firstOrNull {
            (prefix == "" && it.getLocalName() == "xmlns") ||
                    (it.getPrefix() == "xmlns" && it.getLocalName() == prefix)
        }?.getValue() ?: getParentNode()?.myLookupNamespaceURI(prefix)

    }
    else -> null
}
