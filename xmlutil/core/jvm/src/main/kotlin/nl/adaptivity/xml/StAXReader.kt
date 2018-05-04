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

package nl.adaptivity.xml

import java.io.InputStream
import java.io.Reader
import javax.xml.namespace.NamespaceContext
import javax.xml.stream.*
import javax.xml.transform.Source

actual typealias PlatformXmlReader = StAXReader

/**
 * An implementation of [XmlReader] based upon the JDK StAX implementation.
 * @author Created by pdvrieze on 16/11/15.
 */
class StAXReader(private val delegate: XMLStreamReader) : XmlReader {

    override var isStarted = false
        private set

    private var mFixWhitespace = false
    override var depth = 0
        private set


    @Throws(XMLStreamException::class)
    constructor(reader: Reader) : this(XMLInputFactory.newFactory().createXMLStreamReader(reader))

    @Throws(XMLStreamException::class)
    constructor(inputStream: InputStream, encoding: String?) : this(XMLInputFactory.newFactory().createXMLStreamReader(
        inputStream,
        encoding))

    @Throws(XMLStreamException::class)
    constructor(source: Source) : this(XMLInputFactory.newFactory().createXMLStreamReader(source))

    @Throws(XmlException::class)
    override fun close() {
        try {
            delegate.close()
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }

    }

    override fun isEndElement(): Boolean {
        return delegate.isEndElement
    }

    val isStandalone: Boolean
        @Deprecated("")
        get() = standalone ?: false

    override fun isCharacters(): Boolean {
        return delegate.isCharacters
    }

    override fun isStartElement(): Boolean {
        return delegate.isStartElement
    }

    @Throws(XmlException::class)
    override fun isWhitespace(): Boolean {
        return delegate.isWhiteSpace
    }

    val isWhiteSpace: Boolean
        @Deprecated("Use alternative name", ReplaceWith("isWhitespace"))
        @Throws(XmlException::class)
        get() = isWhitespace()

    val namespaceURI: String
        @Deprecated("")
        get() = namespaceUri

    override val namespaceUri: String
        get() = delegate.namespaceURI

    @Deprecated("")
    fun hasText(): Boolean {
        return delegate.hasText()
    }

    @Throws(XmlException::class)
    override fun require(type: EventType, namespace: String?, name: String?) {
        delegate.require(LOCAL_TO_DELEGATE[type.ordinal], namespace, name)
    }

    val namespaceCount: Int
        @Deprecated("Not needed", ReplaceWith("namespaceEnd - namespaceStart"))
        @Throws(XmlException::class)
        get() = namespaceEnd - namespaceStart

    val textCharacters: CharArray
        @Deprecated("", ReplaceWith("text.toCharArray()"))
        get() = text.toCharArray()

    val characterEncodingScheme: String
        @Deprecated("")
        get() = delegate.characterEncodingScheme

    override fun getAttributeName(index: Int): QName {
        return QName(getAttributeNamespace(index), getAttributeLocalName(index), getAttributePrefix(index))
    }

    override fun getNamespaceUri(prefix: String): String? {
        return delegate.getNamespaceURI(prefix)
    }

    fun getNamespaceURI(prefix: String): String? {
        return getNamespaceUri(prefix)
    }

    @Throws(XmlException::class)
    override fun getNamespacePrefix(namespaceUri: String): String? {
        return delegate.namespaceContext.getPrefix(namespaceUri)
    }

    override val locationInfo: String?
        get() {
            val location = delegate.location
            return location?.toString()
        }

    val location: Location
        @Deprecated("")
        get() = delegate.location

    override fun getAttributeValue(nsUri: String?, localName: String): String? {
        return delegate.getAttributeValue(nsUri, localName)
    }

    override val version: String?
        @Deprecated("")
        get() = delegate.version

    override val name: QName
        @Deprecated("")
        get() = QName(namespaceUri, localName, prefix)

    @Throws(XmlException::class)
    override fun next(): EventType {
        isStarted = true
        try {
            return updateDepth(fixWhitespace(delegateToLocal(delegate.next())))
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }

    }

    private fun delegateToLocal(eventType: Int) = DELEGATE_TO_LOCAL[eventType] ?: throw XmlException(
        "Unsupported event type")

    @Throws(XmlException::class)
    override fun nextTag(): EventType {
        isStarted = true
        try {
            return updateDepth(fixWhitespace(delegateToLocal(delegate.nextTag())))
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }

    }

    private fun fixWhitespace(eventType: EventType): EventType {
        if (eventType === EventType.TEXT) {
            if (isXmlWhitespace(delegate.text)) {
                mFixWhitespace = true
                return EventType.IGNORABLE_WHITESPACE
            }
        }
        mFixWhitespace = false
        return eventType
    }

    private fun updateDepth(eventType: EventType) = when (eventType) {
        EventType.START_ELEMENT -> {
            ++depth; eventType
        }
        EventType.END_ELEMENT   -> {
            --depth; eventType
        }
        else                    -> eventType
    }

    @Throws(XmlException::class)
    override fun hasNext(): Boolean {
        try {
            return delegate.hasNext()
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }
    }

    override val attributeCount: Int
        get() = delegate.attributeCount

    override fun getAttributeNamespace(index: Int): String {
        return delegate.getAttributeNamespace(index)
    }

    override fun getAttributeLocalName(index: Int): String {
        return delegate.getAttributeLocalName(index)
    }

    override fun getAttributePrefix(index: Int): String {
        return delegate.getAttributePrefix(index)
    }

    override fun getAttributeValue(index: Int): String {
        return delegate.getAttributeValue(index)
    }

    override val namespaceStart: Int
        get() = 0

    override val namespaceEnd: Int
        @Throws(XmlException::class)
        get() = delegate.namespaceCount

    @Deprecated("Wrong name", ReplaceWith("getNamespaceUri(index)"))
    fun getNamespaceURI(index: Int) = getNamespaceUri(index)

    override fun getNamespaceUri(index: Int): String = delegate.getNamespaceURI(index)

    override fun getNamespacePrefix(index: Int): String = delegate.getNamespacePrefix(index)

    override val namespaceContext: NamespaceContext
        get() = delegate.namespaceContext

    override val eventType: EventType
        get() = if (mFixWhitespace) EventType.IGNORABLE_WHITESPACE else delegateToLocal(delegate.eventType)

    override val text: String
        get() = delegate.text

    override val encoding: String?
        get() = delegate.encoding

    override val localName: String
        get() = delegate.localName

    override val prefix: String
        get() = delegate.prefix

    override val standalone: Boolean?
        get() = if (delegate.standaloneSet()) delegate.isStandalone else null

    companion object {

        private val DELEGATE_TO_LOCAL = Array(16) { i ->
            when (i) {
                XMLStreamConstants.CDATA                  -> EventType.CDSECT
                XMLStreamConstants.COMMENT                -> EventType.COMMENT
                XMLStreamConstants.DTD                    -> EventType.DOCDECL
                XMLStreamConstants.END_DOCUMENT           -> EventType.END_DOCUMENT
                XMLStreamConstants.END_ELEMENT            -> EventType.END_ELEMENT
                XMLStreamConstants.ENTITY_REFERENCE       -> EventType.ENTITY_REF
                XMLStreamConstants.SPACE                  -> EventType.IGNORABLE_WHITESPACE
                XMLStreamConstants.PROCESSING_INSTRUCTION -> EventType.PROCESSING_INSTRUCTION
                XMLStreamConstants.START_DOCUMENT         -> EventType.START_DOCUMENT
                XMLStreamConstants.START_ELEMENT          -> EventType.START_ELEMENT
                XMLStreamConstants.CHARACTERS             -> EventType.TEXT
                XMLStreamConstants.ATTRIBUTE              -> EventType.ATTRIBUTE
                else                                      -> null
            }

        }

        private val LOCAL_TO_DELEGATE = IntArray(12) { i ->
            when (i) {
                EventType.CDSECT.ordinal                 -> XMLStreamConstants.CDATA
                EventType.COMMENT.ordinal                -> XMLStreamConstants.COMMENT
                EventType.DOCDECL.ordinal                -> XMLStreamConstants.DTD
                EventType.END_DOCUMENT.ordinal           -> XMLStreamConstants.END_DOCUMENT
                EventType.END_ELEMENT.ordinal            -> XMLStreamConstants.END_ELEMENT
                EventType.ENTITY_REF.ordinal             -> XMLStreamConstants.ENTITY_REFERENCE
                EventType.IGNORABLE_WHITESPACE.ordinal   -> XMLStreamConstants.SPACE
                EventType.PROCESSING_INSTRUCTION.ordinal -> XMLStreamConstants.PROCESSING_INSTRUCTION
                EventType.START_DOCUMENT.ordinal         -> XMLStreamConstants.START_DOCUMENT
                EventType.START_ELEMENT.ordinal          -> XMLStreamConstants.START_ELEMENT
                EventType.TEXT.ordinal                   -> XMLStreamConstants.CHARACTERS
                EventType.ATTRIBUTE.ordinal              -> XMLStreamConstants.ATTRIBUTE
                else                                     -> -1
            }
        }
    }
}
