/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.siblingsToFragment
import nl.adaptivity.xmlutil.*
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Node


typealias JSCompactFragment = CompactFragment

/**
 * A class representing an xml fragment compactly.
 * Created by pdvrieze on 06/11/15.
 */
actual class CompactFragment : ICompactFragment {

    actual class Factory : XmlDeserializerFactory<CompactFragment> {

        override fun deserialize(reader: XmlReader): CompactFragment {
            return CompactFragment.deserialize(reader)
        }
    }

    override val isEmpty: Boolean
        get() = contentString.isEmpty()


    override val namespaces: IterableNamespaceContext

    @Deprecated("In javascript this is not efficient, use contentString")
    override val content: CharArray
        get() = CharArray(contentString.length) { i -> contentString[i] }

    override val contentString: String

    actual constructor(namespaces: Iterable<Namespace>, content: CharArray?) {
        this.namespaces = SimpleNamespaceContext.from(namespaces)
        this.contentString = content?.toString() ?: ""
    }

    /** Convenience constructor for content without namespaces.  */
    actual constructor(content: String) : this(emptyList(), content)

    constructor(documentFragment: DocumentFragment):this(documentFragment.toString())
    constructor(node: Node):this(node.toString())

    /** Convenience constructor for content without namespaces.  */
    actual constructor(namespaces: Iterable<Namespace>, content: String) {
        this.namespaces = SimpleNamespaceContext.from(namespaces)
        this.contentString = content
    }

    actual constructor(orig: ICompactFragment) {
        namespaces = SimpleNamespaceContext.from(orig.namespaces)
        contentString = orig.contentString
    }

    actual constructor(content: XmlSerializable) {
        namespaces = SimpleNamespaceContext(emptyList())
        contentString = content.toString()
    }

    override fun serialize(out: XmlWriter) {
        XMLFragmentStreamReader.from(this).let { reader: XmlReader ->
            out.serialize(reader)
        }
    }

    override fun getXmlReader(): XmlReader = XMLFragmentStreamReader.from(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        @Suppress("UnsafeCastFromDynamic")
        if (other == null || prototype != other.prototype) return false

        val that = other as ICompactFragment?

        if (namespaces != that!!.namespaces) return false
        return contentString == that.contentString

    }

    override fun hashCode(): Int {
        var result = namespaces.hashCode()
        result = 31 * result + contentString.hashCode()
        return result
    }

    override fun toString(): String {
        return buildString {
            append("{namespaces=[")
            namespaces.joinTo(this) { "\"${it.prefix} -> ${it.namespaceURI}" }

            append("], content=")
                .append(contentString)
                .append('}')
        }
    }


    actual companion object {

        val FACTORY = Factory()

        actual fun deserialize(reader: XmlReader): CompactFragment {
            return reader.siblingsToFragment()
        }
    }
}


/**
 * Helper function that exposes the prototype object of javascript objects.
 */
private val Any.prototype:dynamic get() {

    inline fun prototype(o:dynamic):dynamic {
        return o.prototype
    }

    return prototype(this)
}
