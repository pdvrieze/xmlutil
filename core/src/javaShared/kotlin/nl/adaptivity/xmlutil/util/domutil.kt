/*
 * Copyright (c) 2017.
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

package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.*
import org.w3c.dom.*
import javax.xml.parsers.DocumentBuilderFactory

internal fun createDocument(rootElementName: QName): Document {
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

internal fun Document.createElement(name: QName): Element {
    return createElementNS(name.namespaceURI, name.toCName())
}

internal val Document.firstElementChild: Element?
    get() {
        var c = firstChild
        while (c != null) {
            if (c is Element) return c
            c = c.nextSibling
        }
        return null
    }

internal val Document.childElementCount: Int
    get() {
        var count = 0
        var c = firstChild
        while (c != null) {
            if (c is Element) count++
            c = c.nextSibling
        }
        return count
    }

internal val Node.isElement: Boolean get() = nodeType == Node.ELEMENT_NODE

internal val Node.isText: Boolean
    get() = when (nodeType) {
        Node.ELEMENT_NODE, Node.CDATA_SECTION_NODE -> true
        else -> false
    }

/** Get the parent element if it is an element, and there is one. */
internal val Node.parentElement: Element? get() = parentNode as? Element

internal fun NamedNodeMap.get(index: Int) = item(index)

internal fun NodeList.get(index: Int) = item(index)

internal fun NodeList.asList(): List<Node> {
    return object : AbstractList<Node>() {
        private val delegate: NodeList = this@asList

        override val size: Int
            get() = delegate.getLength()

        override fun get(index: Int): Node = delegate.item(index)
    }
}

/** Allow access to the node as [Element] if it is an element, otherwise it is null. */
internal fun Node.asElement(): Element? = if (isElement) this as Element else null

/** Allow access to the node as [Text], if so, otherwise null. */
internal fun Node.asText(): Text? = if (isText) this as Text else null

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

public operator fun NodeList.iterator(): Iterator<Node> = object : Iterator<Node> {
    private var idx = 0

    override fun hasNext(): Boolean = idx < length

    override fun next(): Node {
        return get(idx)!!.also { idx++ }
    }
}

public operator fun NamedNodeMap.iterator(): Iterator<Node> = object : Iterator<Node> {
    private var idx = 0

    override fun hasNext(): Boolean = idx < length

    override fun next(): Node {
        return (get(idx) as Node).also { idx++ }
    }
}

/** A simple for each implementation for [NamedNodeMap]s. */
public inline fun NamedNodeMap.forEach(body: (Node) -> Unit) {
    val l = getLength()
    for (idx in 0 until l) {
        body(item(idx))
    }
}

/** Implement forEach with a built-in cast to attr. */
internal inline fun NamedNodeMap.forEachAttr(body: (Attr) -> Unit) {
    for (i in this) {
        body(i as Attr)
    }
}

/** A filter function on a [NamedNodeMap] that returns a list of all
 * (attributes)[Attr] that meet the [predicate].
 */
internal inline fun NamedNodeMap.filter(predicate: (Node) -> Boolean): List<Node> {
    val result = mutableListOf<Node>()
    forEach { attr ->
        if (predicate(attr)) result.add(attr)
    }
    return result
}

/** A filter function on a [NamedNodeMap] that returns a list of all
 * (attributes)[Attr] that meet the [predicate].
 */
internal inline fun <reified T : Node> NamedNodeMap.filterTyped(predicate: (T) -> Boolean): List<T> {
    val result = mutableListOf<T>()
    forEach { attr ->
        if (attr is T && predicate(attr)) result.add(attr)
    }
    return result
}

/**
 * A (map)[Collection.map] function for transforming attributes.
 */
internal inline fun <R> NamedNodeMap.map(body: (Node) -> R): List<R> {
    val result = mutableListOf<R>()
    forEach { attr ->
        result.add(body(attr))
    }
    return result
}

/**
 * A function to count all attributes for which the [predicate] holds.
 */
internal inline fun NamedNodeMap.count(predicate: (Node) -> Boolean): Int {
    var count = 0
    forEach { attr ->
        if (predicate(attr)) count++
    }
    return count
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

/** Remove namespaces attributes from a tree that have already been declared by a parent. */
internal fun Node.removeUnneededNamespaces(knownNamespaces: ExtendingNamespaceContext = ExtendingNamespaceContext()) {
    if (nodeType == Node.ELEMENT_NODE) {
        @Suppress("UnsafeCastFromDynamic")
        val elem: Element = this as Element

        val toRemove = mutableListOf<Attr>()

        elem.attributes.forEachAttr { attr ->
            if (attr.prefix == "xmlns") {
                val knownUri = knownNamespaces.parent.getNamespaceURI(attr.localName)
                if (attr.value == knownUri) {
                    toRemove.add(attr)
                } else {
                    knownNamespaces.addNamespace(attr.localName, attr.value)
                }
            } else if (attr.prefix == "" && attr.localName == "xmlns") {
                val knownUri = knownNamespaces.parent.getNamespaceURI("")
                if (attr.value == knownUri) {
                    toRemove.add(attr)
                } else {
                    knownNamespaces.addNamespace("", attr.value)
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
    @OptIn(ExperimentalStdlibApi::class)
    override fun getPrefixes(namespaceURI: String): Iterator<String?> {
        return buildSet {
            localNamespaces.asSequence()
                .filter { it.namespaceURI == namespaceURI }
                .mapTo(this) { it.prefix }
            parent.prefixesFor(namespaceURI).forEach { add(it) }
        }.iterator()
    }

    fun addNamespace(prefix: String, namespaceURI: String) {
        localNamespaces.add(XmlEvent.NamespaceImpl(prefix, namespaceURI))
    }

    fun extend() = ExtendingNamespaceContext(this)
}
