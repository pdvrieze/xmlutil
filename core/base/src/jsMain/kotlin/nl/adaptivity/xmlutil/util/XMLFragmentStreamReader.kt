/*
 * Copyright (c) 2024.
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

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.XmlDelegatingReader


/**
 * This streamreader allows for reading document fragments. It does so by wrapping the reader into a pair of wrapper elements, and then ignoring those on reading.

 * Created by pdvrieze on 04/11/15.
 */
public actual class XMLFragmentStreamReader(
    text: String,
    wrapperNamespaceContext: Iterable<Namespace>
) : XmlDelegatingReader(getDelegate(text, wrapperNamespaceContext)) {

    private class FragmentNamespaceContext : SimpleNamespaceContext {

        val parent: FragmentNamespaceContext?

        constructor(parent: FragmentNamespaceContext?, prefixes: Array<String>, namespaces: Array<String>) :
                super(prefixes, namespaces) {
            this.parent = parent
        }

        constructor(parent: FragmentNamespaceContext?, original: SimpleNamespaceContext) :
                super(original) {
            this.parent = parent
        }

        override fun freeze(): FragmentNamespaceContext = this

        override fun getNamespaceURI(prefix: String): String? {
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
        null, emptyArray(),
        emptyArray()
    )

    init {
        if (delegate.isStarted && delegate.eventType === EventType.START_ELEMENT) extendNamespace()
    }

    override fun next(): EventType {
        val result = delegate.next()

        when (result) {
            EventType.END_DOCUMENT -> return result

            EventType.START_DOCUMENT,
            EventType.PROCESSING_INSTRUCTION,
            EventType.DOCDECL -> return next()

            EventType.START_ELEMENT -> {
                if (WRAPPERNAMESPACE == delegate.namespaceURI) return next()

                extendNamespace()
            }
            EventType.END_ELEMENT -> {
                if (WRAPPERNAMESPACE == delegate.namespaceURI) {
                    return delegate.next()
                }
                localNamespaceContext = localNamespaceContext.parent ?: localNamespaceContext
            }
            else -> {} // Ignore others
        }
        return result
    }


    override fun getNamespaceURI(prefix: String): String? {
        if (WRAPPERPPREFIX == prefix) return null

        return super.getNamespaceURI(prefix)
    }


    override fun getNamespacePrefix(namespaceUri: String): String? {
        if (WRAPPERNAMESPACE == namespaceUri) return null

        return super.getNamespacePrefix(namespaceUri)
    }

    override val namespaceContext: IterableNamespaceContext
        get() = localNamespaceContext


    private fun extendNamespace() {
        val namespaceDecls = delegate.namespaceDecls
        val nscount = namespaceDecls.size
        val prefixes = Array(nscount) { idx -> namespaceDecls[idx].prefix }
        val namespaces = Array(nscount) { idx -> namespaceDecls[idx].namespaceURI }

        localNamespaceContext = FragmentNamespaceContext(localNamespaceContext, prefixes, namespaces)
    }

    public actual companion object {

        private const val WRAPPERPPREFIX = "SDFKLJDSF"
        private const val WRAPPERNAMESPACE = "http://wrapperns"


        private fun getDelegate(
            text: String,
            wrapperNamespaceContext: Iterable<Namespace>
        ): XmlReader {
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

            return xmlStreaming.newReader(actualInput)
        }


        public actual fun from(fragment: ICompactFragment): XMLFragmentStreamReader {
            return XMLFragmentStreamReader(fragment.contentString, fragment.namespaces)
        }
    }


}
