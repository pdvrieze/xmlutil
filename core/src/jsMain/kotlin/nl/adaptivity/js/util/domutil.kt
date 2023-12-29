/*
 * Copyright (c) 2023.
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

package nl.adaptivity.js.util

import kotlinx.dom.isElement
import kotlinx.dom.isText
import org.w3c.dom.*

/** Allow access to the node as [Element] if it is an element, otherwise it is null. */
public fun Node.asElement(): Element? = if (isElement) this as Element else null

/** Allow access to the node as [Text], if so, otherwise null. */
public fun Node.asText(): Text? = if (isText) this as Text else null

/** Remove all the child nodes that are elements. */
public fun Node.removeElementChildren() {
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

public operator fun NamedNodeMap.iterator(): Iterator<Attr> = object : Iterator<Attr> {
    private var idx = 0

    override fun hasNext(): Boolean = idx < length

    override fun next(): Attr {
        return get(idx)!!.also { idx++ }
    }
}

/** A simple for each implementation for [NamedNodeMap]s. */
public inline fun NamedNodeMap.forEach(body: (Attr) -> Unit) {
    for (i in this) {
        body(i)
    }
}

/** A filter function on a [NamedNodeMap] that returns a list of all
 * (attributes)[Attr] that meet the [predicate].
 */
public inline fun NamedNodeMap.filter(predicate: (Attr) -> Boolean): List<Attr> {
    val result = mutableListOf<Attr>()
    forEach { attr ->
        if (predicate(attr)) result.add(attr)
    }
    return result
}

/**
 * A (map)[Collection.map] function for transforming attributes.
 */
public inline fun <R> NamedNodeMap.map(body: (Attr) -> R): List<R> {
    val result = mutableListOf<R>()
    forEach { attr ->
        result.add(body(attr))
    }
    return result
}

/**
 * A function to count all attributes for which the [predicate] holds.
 */
public inline fun NamedNodeMap.count(predicate: (Attr) -> Boolean): Int {
    var count = 0
    forEach { attr ->
        if (predicate(attr)) count++
    }
    return count
}


