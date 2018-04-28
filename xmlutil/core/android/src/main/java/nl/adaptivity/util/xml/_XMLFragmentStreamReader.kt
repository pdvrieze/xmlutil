/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.util.xml

import nl.adaptivity.util.multiplatform.JvmStatic
import nl.adaptivity.xml.*
import java.io.CharArrayReader
import java.io.Reader
import java.io.StringReader
import java.util.*
import javax.xml.XMLConstants


/**
 * This streamreader allows for reading document fragments. It does so by wrapping the reader into a pair of wrapper elements, and then ignoring those on reading.

 * Created by pdvrieze on 04/11/15.
 */
actual class XMLFragmentStreamReader constructor(reader: Reader, namespaces: Iterable<Namespace>)
    : XmlDelegatingReader(XMLFragmentStreamReader.getDelegate(reader, namespaces)) {

    private class FragmentNamespaceContext(val parent: FragmentNamespaceContext?,
                                           prefixes: Array<String>,
                                           namespaces: Array<String>) : SimpleNamespaceContext(prefixes, namespaces) {

        override fun getNamespaceURI(prefix: String): String {
            val namespaceURI = super.getNamespaceURI(prefix)
            parent?.let { return it.getNamespaceURI(prefix) }

            return namespaceURI
        }

        override fun getPrefix(namespaceURI: String): String? {

            val prefix = super.getPrefix(namespaceURI)
            if (prefix == null && parent != null) {
                return parent.getPrefix(namespaceURI)
            }
            return prefix

        }

        override fun getPrefixes(namespaceURI: String): Iterator<String> {
            if (parent == null) {
                return super.getPrefixes(namespaceURI)
            }
            val prefixes = HashSet<String>()

            run {
                val it = super.getPrefixes(namespaceURI)
                while (it.hasNext()) {
                    prefixes.add(it.next())
                }
            }

            val it = parent.getPrefixes(namespaceURI)
            while (it.hasNext()) {
                val prefix = it.next()
                val localNamespaceUri = getLocalNamespaceUri(prefix)
                if (localNamespaceUri == null) {
                    prefixes.add(prefix)
                }
            }

            return prefixes.iterator()
        }

        private fun getLocalNamespaceUri(prefix: String): String? {
            for (i in size - 1 downTo 0) {
                if (prefix == getPrefix(i)) {
                    return getNamespaceURI(i)
                }
            }
            return null
        }
    }

    private var localNamespaceContext: FragmentNamespaceContext = FragmentNamespaceContext(
        null, emptyArray(), emptyArray<String>())

    init {
        if (delegate.eventType === EventType.START_ELEMENT) extendNamespace()
    }

    @Throws(XmlException::class)
    override fun next(): EventType {
        val delegateNext = delegate.next()
        return when (delegateNext) {
            EventType.END_DOCUMENT  -> delegateNext
            EventType.START_DOCUMENT,
            EventType.PROCESSING_INSTRUCTION,
            EventType.DOCDECL       -> next()
            EventType.START_ELEMENT -> {
                if (WRAPPERNAMESPACE.contentEquals(delegate.namespaceUri)) {
                    // Special case the wrapping namespace, dropping the element.
                    next()
                } else {
                    extendNamespace()
                    delegateNext
                }
            }
            EventType.END_ELEMENT   -> if (WRAPPERNAMESPACE.contentEquals(delegate.namespaceUri)) {
                // Drop the closing tag of the wrapper as well
                delegate.next()
            } else {
                localNamespaceContext = localNamespaceContext.parent ?: localNamespaceContext
                delegateNext
            }
            else                    -> delegateNext
        }
    }

    @Throws(XmlException::class)
    override fun getNamespaceUri(prefix: CharSequence): String? {
        if (WRAPPERPPREFIX.contentEquals(prefix)) return null

        return super.getNamespaceUri(prefix)
    }

    @Throws(XmlException::class)
    override fun getNamespacePrefix(namespaceUri: CharSequence): CharSequence? {
        if (WRAPPERNAMESPACE.contentEquals(namespaceUri)) return null

        return super.getNamespacePrefix(namespaceUri)
    }

    override val namespaceStart: Int
        @Throws(XmlException::class)
        get() = 0

    override val namespaceEnd: Int
        @Throws(XmlException::class)
        get() = localNamespaceContext.size

    @Throws(XmlException::class)
    override fun getNamespacePrefix(i: Int): CharSequence {
        return localNamespaceContext.getPrefix(i)
    }

    @Throws(XmlException::class)
    override fun getNamespaceUri(i: Int): CharSequence {
        return localNamespaceContext.getNamespaceURI(i)
    }

    override val namespaceContext: NamespaceContext
        @Throws(XmlException::class)
        get() = localNamespaceContext

    @Throws(XmlException::class)
    private fun extendNamespace() {
        val nsStart = delegate.namespaceStart
        val nscount = delegate.namespaceEnd - nsStart
        val prefixes = Array<String>(nscount) { idx -> delegate.getNamespacePrefix(idx + nsStart).toString() }
        val namespaces = Array<String>(nscount) { idx -> delegate.getNamespaceUri(idx + nsStart).toString() }

        localNamespaceContext = FragmentNamespaceContext(
            localNamespaceContext, prefixes, namespaces)
    }

    actual companion object {

        private val WRAPPERPPREFIX = "SDFKLJDSF"
        private val WRAPPERNAMESPACE = "http://wrapperns"

        @Throws(XmlException::class)
        private fun getDelegate(reader: Reader,
                                wrapperNamespaceContext: Iterable<nl.adaptivity.xml.Namespace>): XmlReader {
            val wrapper = buildString {
                append("<$WRAPPERPPREFIX:wrapper xmlns:$WRAPPERPPREFIX=\"$WRAPPERNAMESPACE\"")
                for (ns in wrapperNamespaceContext) {
                    val prefix = ns.prefix
                    val uri = ns.namespaceURI
                    if (XMLConstants.DEFAULT_NS_PREFIX == prefix) {
                        append(" xmlns")
                    } else {
                        append(" xmlns:").append(prefix)
                    }
                    append("=\"").append(uri.xmlEncode()).append('"')
                }
                append(" >")
            }

            val actualInput = CombiningReader(StringReader(wrapper), reader, StringReader("</$WRAPPERPPREFIX:wrapper>"))
            return XmlStreaming.newReader(actualInput)
        }

        @Throws(XmlException::class)
        @JvmStatic
        fun from(reader: Reader, namespaceContext: Iterable<nl.adaptivity.xml.Namespace>): XMLFragmentStreamReader {
            return XMLFragmentStreamReader(reader, namespaceContext)
        }

        @Throws(XmlException::class)
        @JvmStatic
        fun from(reader: Reader): XMLFragmentStreamReader {
            return XMLFragmentStreamReader(reader, emptyList<nl.adaptivity.xml.Namespace>())
        }

        @Throws(XmlException::class)
        @JvmStatic
        actual fun from(fragment: CompactFragment): XMLFragmentStreamReader {
            return XMLFragmentStreamReader(CharArrayReader(fragment.content), fragment.namespaces)
        }
    }


}
