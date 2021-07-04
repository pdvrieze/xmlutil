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

package nl.adaptivity.xmlutil

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.Reader

/**
 * Type alias that allows cross-platform reference to the platform reader.
 */
actual typealias PlatformXmlReader = AndroidXmlReader


/**
 * And XMLReader implementation that works on Android
 */
class AndroidXmlReader(val parser: XmlPullParser) : XmlReader {
    override var isStarted: Boolean = false
        private set

    private constructor() : this(XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser())

    constructor(reader: Reader) : this() {
        parser.setInput(reader)
    }

    constructor(input: InputStream, encoding: String) : this() {
        parser.setInput(input, encoding)
    }

    override val eventType: EventType
        get() = DELEGATE_TO_LOCAL[parser.eventType]

    override fun getAttributeValue(nsUri: String?, localName: String): String? {
        return parser.getAttributeValue(nsUri, localName)
    }

    override fun isWhitespace(): Boolean = parser.isWhitespace

    @Throws(XmlException::class)
    override fun hasNext(): Boolean {
        // TODO make this more robust (if needed)
        return eventType !== EventType.END_DOCUMENT
    }

    @Throws(XmlException::class)
    override fun next(): EventType = DELEGATE_TO_LOCAL[parser.nextToken()].also { isStarted = true }

    @Throws(XmlException::class)
    override fun nextTag(): EventType = DELEGATE_TO_LOCAL[parser.nextTag()].also { isStarted = true }

    override val depth: Int
        get() = parser.depth

    override val text: String
        get() = parser.text

    override val localName: String
        get() = parser.name

    override val namespaceURI: String
        get() = parser.namespace ?: XMLConstants.NULL_NS_URI

    override val prefix: String
        get() = parser.prefix ?: XMLConstants.DEFAULT_NS_PREFIX

    override val attributeCount: Int
        get() = parser.attributeCount

    override fun getAttributeLocalName(index: Int): String = parser.getAttributeName(index)

    override fun getAttributePrefix(index: Int): String = parser.getAttributePrefix(index)
        ?: XMLConstants.DEFAULT_NS_PREFIX

    override fun getAttributeValue(index: Int): String = parser.getAttributeValue(index)

    override fun getAttributeNamespace(index: Int): String = parser.getAttributeNamespace(index)
        ?: XMLConstants.NULL_NS_URI

    override fun getNamespaceURI(prefix: String): String? {
        for (i in parser.getNamespaceCount(parser.depth) downTo 0) {
            if (prefix == parser.getNamespacePrefix(i)) {
                return parser.getNamespaceUri(i)
            }
        }

        if (prefix.isEmpty()) {
            return XMLConstants.NULL_NS_URI
        }
        return null
    }

    override fun getNamespacePrefix(namespaceUri: String): String? {
        if (namespaceUri.isEmpty()) {
            return XMLConstants.DEFAULT_NS_PREFIX
        }
        for (i in parser.getNamespaceCount(parser.depth) downTo 0) {
            if (namespaceUri == parser.getNamespaceUri(i)) {
                return parser.getNamespacePrefix(i)
            }
        }

        return null
    }

    override val locationInfo: String
        get() = buildString { 
            append(parser.lineNumber)
            append(':')
            append(parser.columnNumber)
        }

    override val standalone: Boolean
        get() = parser.getProperty("xmldecl-standalone") as Boolean

    override val encoding: String?
        get() = parser.inputEncoding

    override val version: String?
        get() = null

    /**
     * This method creates a new immutable context, so keeping the context around is valid. For
     * reduced perfomance overhead use [.getNamespacePrefix] and [.getNamespaceUri]
     * for lookups.
     */
    override val namespaceContext: IterableNamespaceContext
        get() {
            val nsCount = parser.getNamespaceCount(parser.depth)
            val prefixes = Array(nsCount) { i -> parser.getNamespacePrefix(i) ?: "" }
            val uris = Array(nsCount) { i -> parser.getNamespaceUri(i) ?: "" }

            return SimpleNamespaceContext(prefixes, uris)
        }

    @Throws(XmlException::class)
    override fun close() {
        /* Does nothing in this implementation */
    }

    private companion object {

        @Suppress("UNCHECKED_CAST")
        private val DELEGATE_TO_LOCAL = arrayOfNulls<EventType>(11) as Array<EventType>

        private val LOCAL_TO_DELEGATE: IntArray = IntArray(12)

        init {
            DELEGATE_TO_LOCAL[XmlPullParser.CDSECT] = EventType.CDSECT
            DELEGATE_TO_LOCAL[XmlPullParser.COMMENT] = EventType.COMMENT
            DELEGATE_TO_LOCAL[XmlPullParser.DOCDECL] = EventType.DOCDECL
            DELEGATE_TO_LOCAL[XmlPullParser.END_DOCUMENT] = EventType.END_DOCUMENT
            DELEGATE_TO_LOCAL[XmlPullParser.END_TAG] = EventType.END_ELEMENT
            DELEGATE_TO_LOCAL[XmlPullParser.ENTITY_REF] = EventType.ENTITY_REF
            DELEGATE_TO_LOCAL[XmlPullParser.IGNORABLE_WHITESPACE] = EventType.IGNORABLE_WHITESPACE
            DELEGATE_TO_LOCAL[XmlPullParser.PROCESSING_INSTRUCTION] = EventType.PROCESSING_INSTRUCTION
            DELEGATE_TO_LOCAL[XmlPullParser.START_DOCUMENT] = EventType.START_DOCUMENT
            DELEGATE_TO_LOCAL[XmlPullParser.START_TAG] = EventType.START_ELEMENT
            DELEGATE_TO_LOCAL[XmlPullParser.TEXT] = EventType.TEXT

            LOCAL_TO_DELEGATE[EventType.CDSECT.ordinal] = XmlPullParser.CDSECT
            LOCAL_TO_DELEGATE[EventType.COMMENT.ordinal] = XmlPullParser.COMMENT
            LOCAL_TO_DELEGATE[EventType.DOCDECL.ordinal] = XmlPullParser.DOCDECL
            LOCAL_TO_DELEGATE[EventType.END_DOCUMENT.ordinal] = XmlPullParser.END_DOCUMENT
            LOCAL_TO_DELEGATE[EventType.END_ELEMENT.ordinal] = XmlPullParser.END_TAG
            LOCAL_TO_DELEGATE[EventType.ENTITY_REF.ordinal] = XmlPullParser.ENTITY_REF
            LOCAL_TO_DELEGATE[EventType.IGNORABLE_WHITESPACE.ordinal] = XmlPullParser.IGNORABLE_WHITESPACE
            LOCAL_TO_DELEGATE[EventType.PROCESSING_INSTRUCTION.ordinal] = XmlPullParser.PROCESSING_INSTRUCTION
            LOCAL_TO_DELEGATE[EventType.START_DOCUMENT.ordinal] = XmlPullParser.START_DOCUMENT
            LOCAL_TO_DELEGATE[EventType.START_ELEMENT.ordinal] = XmlPullParser.START_TAG
            LOCAL_TO_DELEGATE[EventType.TEXT.ordinal] = XmlPullParser.TEXT
            LOCAL_TO_DELEGATE[EventType.ATTRIBUTE.ordinal] = Integer.MIN_VALUE
        }
    }
}
