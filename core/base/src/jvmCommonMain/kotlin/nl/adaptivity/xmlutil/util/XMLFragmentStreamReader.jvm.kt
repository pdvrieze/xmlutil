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

package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.XmlDelegatingReader
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.util.impl.FragmentNamespaceContext
import java.io.CharArrayReader
import java.io.Reader
import java.io.StringReader

/**
 * This streamreader allows for reading document fragments. It does so by wrapping the reader into a
 * pair of wrapper elements, and then ignoring those on reading. This avoids the restriction of
 * xml that a document only has 1 document element.
 *
 * @author Created by pdvrieze on 04/11/15.
 *
 * @param reader The reader to read the "fragment" from.
 * @param namespaces A list of namespace/prefix declarations to resolve for the fragment.
 */
@Suppress("DEPRECATION")
public actual class XMLFragmentStreamReader private constructor(delegate: XmlReader) :
    XmlDelegatingReader(delegate), XMLFragmentStreamReaderJava {

    override val delegate: XmlReader get() = super.delegate

    public var localNamespaceContext: FragmentNamespaceContext =
        FragmentNamespaceContext(null, emptyArray(), emptyArray())

    public constructor(reader: Reader, namespaces: Iterable<Namespace>) : this(getDelegate(reader, namespaces)) {
        if (delegate.isStarted && delegate.eventType === EventType.START_ELEMENT) extendNamespace()
    }

    override fun getNamespaceURI(prefix: String): String? {
        if (WRAPPERPPREFIX.contentEquals(prefix)) return null

        return super.getNamespaceURI(prefix)
    }

    override fun getNamespacePrefix(namespaceUri: String): String? {
        if (WRAPPERNAMESPACE.contentEquals(namespaceUri)) return null

        return super.getNamespacePrefix(namespaceUri)
    }

    override fun next(): EventType {
        return when (val delegateNext = delegate.next()) {
            EventType.END_DOCUMENT -> delegateNext

            EventType.START_DOCUMENT,
            EventType.PROCESSING_INSTRUCTION,
            EventType.DOCDECL -> next()

            EventType.START_ELEMENT -> when {
                // Special case the wrapping namespace, dropping the element.
                WRAPPERNAMESPACE.contentEquals(delegate.namespaceURI) -> next()

                else -> {
                    extendNamespace()
                    delegateNext
                }
            }

            EventType.END_ELEMENT -> when {
                // Drop the closing tag of the wrapper as well
                WRAPPERNAMESPACE.contentEquals(delegate.namespaceURI) -> delegate.next()

                else -> {
                    localNamespaceContext = localNamespaceContext.parent ?: localNamespaceContext
                    delegateNext
                }
            }

            else -> delegateNext
        }
    }

    override val namespaceContext: IterableNamespaceContext get() = localNamespaceContext

    public actual companion object {
        private const val WRAPPERPPREFIX: String = "SDFKLJDSF"
        private const val WRAPPERNAMESPACE: String = "http://wrapperns"

        private fun getDelegate(
            reader: Reader,
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

            val actualInput = CombiningReader(StringReader(wrapper), reader, StringReader("</$WRAPPERPPREFIX:wrapper>"))
            return KtXmlReader(reader=actualInput, relaxed=false)
        }

        @JvmStatic
        public fun from(reader: Reader, namespaceContext: Iterable<Namespace>): XMLFragmentStreamReader {
            return XMLFragmentStreamReader(reader, namespaceContext)
        }

        @JvmStatic
        public fun from(reader: Reader): XMLFragmentStreamReader {
            return XMLFragmentStreamReader(reader, emptyList())
        }

        @JvmStatic
        public actual fun from(fragment: ICompactFragment): XMLFragmentStreamReader {
            return XMLFragmentStreamReader(CharArrayReader(fragment.content), fragment.namespaces)
        }

    }

    private fun extendNamespace() {
        val namespaceDecls = delegate.namespaceDecls
        val nscount = namespaceDecls.size
        val prefixes = Array(nscount) { idx -> namespaceDecls[idx].prefix }
        val namespaces = Array(nscount) { idx -> namespaceDecls[idx].namespaceURI }

        localNamespaceContext = FragmentNamespaceContext(localNamespaceContext, prefixes, namespaces)
    }


}

