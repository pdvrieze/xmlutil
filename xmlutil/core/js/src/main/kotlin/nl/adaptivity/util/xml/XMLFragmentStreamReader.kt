/*
 * Copyright (c) 2017. 
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

import nl.adaptivity.xml.*
import org.w3c.dom.parsing.DOMParser


/**
 * This streamreader allows for reading document fragments. It does so by wrapping the reader into a pair of wrapper elements, and then ignoring those on reading.

 * Created by pdvrieze on 04/11/15.
 */
actual class XMLFragmentStreamReader constructor(text: String,
                                                 wrapperNamespaceContext: Iterable<Namespace>)
    : XmlDelegatingReader(XMLFragmentStreamReader.getDelegate(text, wrapperNamespaceContext)) {

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

        @Suppress("OverridingDeprecatedMember", "DEPRECATION")
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

    private var localNamespaceContext: FragmentNamespaceContext = FragmentNamespaceContext(null, emptyArray(),
                                                                                           emptyArray())

    init {
        if (delegate.eventType === EventType.START_ELEMENT) extendNamespace()
    }

    override fun next(): EventType {
        val result = delegate.next()

        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (result) {
            EventType.END_DOCUMENT  -> return result

            EventType.START_DOCUMENT,
            EventType.PROCESSING_INSTRUCTION,
            EventType.DOCDECL       -> return next()

            EventType.START_ELEMENT -> {
                if (WRAPPERNAMESPACE == delegate.namespaceUri) return next()

                extendNamespace()
            }
            EventType.END_ELEMENT   -> {
                if (WRAPPERNAMESPACE == delegate.namespaceUri) {
                    return delegate.next()
                }
                localNamespaceContext = localNamespaceContext.parent ?: localNamespaceContext
            }
        }
        return result
    }


    override fun getNamespaceUri(prefix: String): String? {
        if (WRAPPERPPREFIX == prefix) return null

        return super.getNamespaceUri(prefix)
    }


    override fun getNamespacePrefix(namespaceUri: String): String? {
        if (WRAPPERNAMESPACE == namespaceUri) return null

        return super.getNamespacePrefix(namespaceUri)
    }

    override val namespaceStart: Int
        get() = 0

    override val namespaceEnd: Int
        get() = localNamespaceContext.size


    override fun getNamespacePrefix(index: Int): String {
        return localNamespaceContext.getPrefix(index)
    }


    override fun getNamespaceUri(index: Int): String {
        return localNamespaceContext.getNamespaceURI(index)
    }

    override val namespaceContext: NamespaceContext
        get() = localNamespaceContext


    private fun extendNamespace() {
        val nsStart = delegate.namespaceStart
        val nscount = delegate.namespaceEnd - nsStart
        val prefixes = Array(nscount) { idx -> delegate.getNamespacePrefix(idx + nsStart) }
        val namespaces = Array(nscount) { idx -> delegate.getNamespaceUri(idx + nsStart) }

        localNamespaceContext = FragmentNamespaceContext(localNamespaceContext, prefixes, namespaces)
    }

    actual companion object {

        private const val WRAPPERPPREFIX = "SDFKLJDSF"
        private const val WRAPPERNAMESPACE = "http://wrapperns"


        private fun getDelegate(text: String,
                                wrapperNamespaceContext: Iterable<Namespace>): XmlReader {
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

            val actualInput = "$wrapper$text</$WRAPPERPPREFIX:wrapper>"
            val parser = DOMParser()
            return JSDomReader(parser.parseFromString(actualInput, "text/xml"))
        }


        actual fun from(fragment: ICompactFragment): XMLFragmentStreamReader {
            return XMLFragmentStreamReader(fragment.contentString, fragment.namespaces)
        }
    }


}
