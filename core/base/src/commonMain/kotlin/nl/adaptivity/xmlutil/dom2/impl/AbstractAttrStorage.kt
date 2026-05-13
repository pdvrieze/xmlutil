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

package nl.adaptivity.xmlutil.dom2.impl

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom.PlatformAttr
import nl.adaptivity.xmlutil.dom.PlatformNamedNodeMap
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom.getNamedItemNS
import nl.adaptivity.xmlutil.dom.length
import nl.adaptivity.xmlutil.dom2.NamedNodeMap
import nl.adaptivity.xmlutil.dom2.localName
import nl.adaptivity.xmlutil.dom2.prefix

@ExperimentalXmlUtilApi
public abstract class AbstractAttrStorage<out A: AbstractAttr<*,*>>(
    private val adapter: Adapter<A>
): NamedNodeMap {
    abstract override val size: Int

    @Deprecated("Use size instead", replaceWith = ReplaceWith("size"), level = DeprecationLevel.WARNING)
    final override fun getLength(): Int = size

    final override fun get(index: Int): A? = item(index)

    abstract override fun item(index: Int): A?

    override fun iterator(): Iterator<A> = AttrIterator()

    override fun getNamedItem(qualifiedName: String): A? = iterator().asSequence().firstOrNull {
        it.getName() == qualifiedName
    }

    override fun getNamedItemNS(namespace: String?, localName: String): A? = iterator().asSequence().firstOrNull {
        (it.getNamespaceURI() ?: "") == (namespace ?: "") && it.getLocalName() == localName
    }

    protected open fun getAttrIndex(name: String): Int {
        val idx = name.indexOf(':')
        return when {
            idx < 0 -> indexOfFirst { it.localName == name }
            else -> {
                val prefix = name.substring(0, idx)
                val localName = name.substring(idx + 1)
                indexOfFirst { it.localName == localName && it.prefix == prefix }
            }
        }
    }

    protected open fun getAttrIndex(namespace: String, localName: String): Int {
        return indexOfFirst { it.localName == localName && (it.getNamespaceURI() ?: "") == namespace }
    }

    public abstract operator fun set(elementIdx: Int, newAttr: @UnsafeVariance A): A?

    public abstract fun removeAttrAt(elementIdx: Int): A?

    public fun removeAttr(attr: PlatformAttr): A {
        val a = adapter.checkAttr(attr)
        return when (a.getNamespaceURI()) {
            null -> removeNamedItem(a.getName())
            else -> removeNamedItemNS(a.getNamespaceURI(), a.getLocalName())
        } ?: throw DOMException.notFoundErr("Missing attribute for removal")
    }



    @IgnorableReturnValue
    override fun setNamedItem(attr: PlatformNode): A? {
        val a = adapter.checkAttr(attr)

        return set(getAttrIndex(a.getName()), a)
    }

    @IgnorableReturnValue
    override fun setNamedItemNS(attr: PlatformNode): A? {
        val a = adapter.checkAttr(attr)

        return set(getAttrIndex(a.getNamespaceURI() ?: "", a.getLocalName()), a)
    }

    @IgnorableReturnValue
    override fun removeNamedItem(qualifiedName: String): A? {
        return removeAttrAt(getAttrIndex(qualifiedName))
    }

    @IgnorableReturnValue
    override fun removeNamedItemNS(namespace: String?, localName: String): A? {
        return removeAttrAt(getAttrIndex((namespace ?: ""), localName))
    }

    public fun isEqualNodes(attributes: PlatformNamedNodeMap): Boolean {
        if (size != attributes.length) return false
        for (left in this) {
            val right = attributes.getNamedItemNS(left.getNamespaceURI(), left.getLocalName())
            if (right == null || !left.isEqualNode(right)) return false
        }
        return true
    }

    internal inner class AttrIterator : Iterator<A> {
        private var pos = 0

        override fun hasNext(): Boolean = pos < size

        override fun next(): A = get(pos++)!!
    }

    @XmlUtilInternal
    public fun interface Adapter<A : AbstractAttr<*, *>> {
        public fun checkAttr(a: PlatformNode): A
    }

}

@ExperimentalXmlUtilApi
public class LinearAttrStorage<out A: AbstractAttr<*,*>>(adapter : Adapter<A>): AbstractAttrStorage<A>(adapter) {
    private val elements = mutableListOf<A>()

    override val size: Int get() = elements.size

    override fun item(index: Int): A? = when {
        index in elements.indices -> elements[index]
        else -> null
    }

    override fun set(elementIdx: Int, newAttr: @UnsafeVariance A): A? {
        return when {
            elementIdx in elements.indices -> elements[elementIdx].also { elements[elementIdx] = newAttr }
            else -> {
                elements.add(newAttr)
                null
            }
        }
    }

    override fun removeAttrAt(elementIdx: Int): A? {
        return when {
            elementIdx in elements.indices -> elements.removeAt(elementIdx)
            else -> null
        }
    }
}
