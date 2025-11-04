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

package nl.adaptivity.xmlutil.util.impl

import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.NamespaceContext
import nl.adaptivity.xmlutil.SimpleNamespaceContext
import nl.adaptivity.xmlutil.XmlEvent
import nl.adaptivity.xmlutil.dom.*
import nl.adaptivity.xmlutil.util.forEachAttr
import nl.adaptivity.xmlutil.util.isElement
import nl.adaptivity.xmlutil.util.isText

/*
@XmlUtilInternal
public actual fun createDocument(rootElementName: QName): Document {
    return DocumentBuilderFactory
        .newInstance()
        .apply { isNamespaceAware = true }
        .newDocumentBuilder()
        .newDocument()
        .also { doc ->
            val rootElement = doc.createElement(rootElementName)
            doc.appendChild(rootElement)
        }
}
*/

internal val PlatformDocument.firstElementChild: PlatformElement?
    get() {
        var c = firstChild
        while (c != null) {
            if (c is PlatformElement) return c
            c = c.nextSibling
        }
        return null
    }

internal val PlatformDocument.childElementCount: Int
    get() {
        var count = 0
        var c = firstChild
        while (c != null) {
            if (c is PlatformElement) count++
            c = c.nextSibling
        }
        return count
    }

internal fun PlatformNodeList.asList(): List<PlatformNode> {
    return object : AbstractList<PlatformNode>() {
        private val delegate: PlatformNodeList = this@asList

        override val size: Int
            get() = delegate.getLength()

        override fun get(index: Int): PlatformNode = delegate.item(index)
    }
}

/** Allow access to the node as [PlatformElement] if it is an element, otherwise it is null. */
internal fun PlatformNode.asElement(): PlatformElement? = if (isElement) this as PlatformElement else null

/** Allow access to the node as [PlatformText], if so, otherwise null. */
internal fun PlatformNode.asText(): PlatformText? = if (isText) this as PlatformText else null

/** A filter function on a [PlatformNamedNodeMap] that returns a list of all
 * (attributes)[PlatformAttr] that meet the [predicate].
 */
internal inline fun PlatformNamedNodeMap.filter(predicate: (PlatformNode) -> Boolean): List<PlatformNode> {
    val result = mutableListOf<PlatformNode>()
    forEachAttr { attr ->
        if (predicate(attr)) result.add(attr)
    }
    return result
}

/**
 * A (map)[Collection.map] function for transforming attributes.
 */
internal inline fun <R> PlatformNamedNodeMap.map(body: (PlatformNode) -> R): List<R> {
    val result = mutableListOf<R>()
    forEachAttr { attr ->
        result.add(body(attr))
    }
    return result
}

/**
 * A function to count all attributes for which the [predicate] holds.
 */
internal inline fun PlatformNamedNodeMap.count(predicate: (PlatformNode) -> Boolean): Int {
    var count = 0
    forEachAttr { attr ->
        if (predicate(attr)) count++
    }
    return count
}

/** Remove namespaces attributes from a tree that have already been declared by a parent. */
internal fun PlatformNode.removeUnneededNamespaces(knownNamespaces: ExtendingNamespaceContext = ExtendingNamespaceContext()) {
    if (nodeType == NodeConsts.ELEMENT_NODE) {
        @Suppress("UnsafeCastFromDynamic")
        val elem: PlatformElement = this as PlatformElement

        val toRemove = mutableListOf<PlatformAttr>()

        elem.attributes.forEachAttr { attr ->
            if (attr.getPrefix() == "xmlns") {
                val knownUri = knownNamespaces.parent.getNamespaceURI(attr.getLocalName())
                if (attr.getValue() == knownUri) {
                    toRemove.add(attr)
                } else {
                    knownNamespaces.addNamespace(attr.getLocalName(), attr.getValue())
                }
            } else if (attr.getPrefix() == "" && attr.getLocalName() == "xmlns") {
                val knownUri = knownNamespaces.parent.getNamespaceURI("")
                if (attr.getValue() == knownUri) {
                    toRemove.add(attr)
                } else {
                    knownNamespaces.addNamespace("", attr.getValue())
                }
            }
        }
        for (attr in toRemove) {
            elem.removeAttributeNode(attr)
        }
        for (child in elem.childNodes.asList()) {
            child.removeUnneededNamespaces(knownNamespaces.extend())
        }
    }
}

internal class ExtendingNamespaceContext(val parent: NamespaceContext = SimpleNamespaceContext("", "")) :
    NamespaceContext {

    private val localNamespaces = mutableListOf<Namespace>()

    override fun getNamespaceURI(prefix: String): String? {
        return localNamespaces.firstOrNull { it.prefix == prefix }?.namespaceURI ?: parent.getNamespaceURI(prefix)
    }

    override fun getPrefix(namespaceURI: String): String? {
        return localNamespaces.firstOrNull { it.namespaceURI == namespaceURI }?.prefix ?: parent.getPrefix(namespaceURI)
    }

    @Suppress("OverridingDeprecatedMember")
    override fun getPrefixes(namespaceURI: String): Iterator<String?> {
        return buildSet {
            localNamespaces.asSequence()
                .filter { it.namespaceURI == namespaceURI }
                .mapTo(this) { it.prefix }
            parent.getPrefixes(namespaceURI).forEach { add(it) }
        }.iterator()
    }

    fun addNamespace(prefix: String, namespaceURI: String) {
        localNamespaces.add(XmlEvent.NamespaceImpl(prefix, namespaceURI))
    }

    fun extend() = ExtendingNamespaceContext(this)
}
