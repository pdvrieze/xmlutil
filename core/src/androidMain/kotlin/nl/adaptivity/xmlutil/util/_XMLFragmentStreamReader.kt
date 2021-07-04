/*
 * Copyright (c) 2018.
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
import nl.adaptivity.xmlutil.XmlDelegatingReader
import nl.adaptivity.xmlutil.util.XMLFragmentStreamReaderJava.Companion.WRAPPERNAMESPACE
import nl.adaptivity.xmlutil.util.XMLFragmentStreamReaderJava.Companion.WRAPPERPPREFIX
import nl.adaptivity.xmlutil.util.impl.FragmentNamespaceContext
import java.io.CharArrayReader
import java.io.Reader
import java.io.StringReader
import javax.xml.XMLConstants

/**
 * This streamreader allows for reading document fragments. It does so by wrapping the reader into a pair of wrapper elements, and then ignoring those on reading.

 * Created by pdvrieze on 04/11/15.
 */
public actual class XMLFragmentStreamReader constructor(reader: Reader, namespaces: Iterable<Namespace>) :
    XmlDelegatingReader(getDelegate(reader, namespaces)), XMLFragmentStreamReaderJava {

    override val delegate: XmlReader get() = super.delegate

    override var localNamespaceContext: FragmentNamespaceContext = FragmentNamespaceContext(
        null, emptyArray(), emptyArray()
    )

    init {
        if (delegate.eventType === EventType.START_ELEMENT) extendNamespace()
    }

    override fun getNamespaceURI(prefix: String): String? {
        if (WRAPPERPPREFIX.contentEquals(prefix)) return null

        return super<XmlDelegatingReader>.getNamespaceURI(prefix)
    }

    override fun getNamespacePrefix(namespaceUri: String): String? {
        if (WRAPPERNAMESPACE.contentEquals(namespaceUri)) return null

        return super<XmlDelegatingReader>.getNamespacePrefix(namespaceUri)
    }

    override fun next(): EventType = super<XMLFragmentStreamReaderJava>.next()

    override val namespaceContext: IterableNamespaceContext get() = super<XMLFragmentStreamReaderJava>.namespaceContext

    public actual companion object {

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
            return XmlStreaming.newReader(actualInput)
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


}
