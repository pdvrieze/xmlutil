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
import nl.adaptivity.util.xml.XMLFragmentStreamReaderJava.Companion.WRAPPERNAMESPACE
import nl.adaptivity.util.xml.XMLFragmentStreamReaderJava.Companion.WRAPPERPPREFIX
import nl.adaptivity.util.xml.impl.FragmentNamespaceContext
import nl.adaptivity.xml.*
import java.io.CharArrayReader
import java.io.Reader
import java.io.StringReader
import javax.xml.XMLConstants

/**
 * This streamreader allows for reading document fragments. It does so by wrapping the reader into a pair of wrapper elements, and then ignoring those on reading.

 * Created by pdvrieze on 04/11/15.
 */
actual class XMLFragmentStreamReader constructor(reader: Reader, namespaces: Iterable<Namespace>)
    : XmlDelegatingReader(XMLFragmentStreamReader.getDelegate(reader, namespaces)), XMLFragmentStreamReaderJava {

    override val delegate: XmlReader get() = super.delegate

    override var localNamespaceContext: FragmentNamespaceContext = FragmentNamespaceContext(
        null, emptyArray(), emptyArray())

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

    override fun next() = super<XMLFragmentStreamReaderJava>.next()

    override val namespaceStart: Int
        get() = super<XMLFragmentStreamReaderJava>.namespaceStart

    override val namespaceEnd: Int
        get() = super<XMLFragmentStreamReaderJava>.namespaceEnd

    override fun getNamespacePrefix(index: Int): String = super<XMLFragmentStreamReaderJava>.getNamespacePrefix(index)

    override fun getNamespaceURI(index: Int): String = super<XMLFragmentStreamReaderJava>.getNamespaceURI(index)

    override val namespaceContext get() = super<XMLFragmentStreamReaderJava>.namespaceContext

    actual companion object {

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

        @JvmStatic
        fun from(reader: Reader, namespaceContext: Iterable<nl.adaptivity.xml.Namespace>): XMLFragmentStreamReader {
            return XMLFragmentStreamReader(reader, namespaceContext)
        }

        @JvmStatic
        fun from(reader: Reader): XMLFragmentStreamReader {
            return XMLFragmentStreamReader(reader, emptyList())
        }

        @JvmStatic
        actual fun from(fragment: ICompactFragment): XMLFragmentStreamReader {
            return XMLFragmentStreamReader(CharArrayReader(fragment.content), fragment.namespaces)
        }
    }


}
