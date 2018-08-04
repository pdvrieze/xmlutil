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

@file:JvmMultifileClass
@file:JvmName("XmlReaderUtil")

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.multiplatform.Closeable
import nl.adaptivity.xmlutil.multiplatform.JvmMultifileClass
import nl.adaptivity.xmlutil.multiplatform.JvmName
import nl.adaptivity.xmlutil.multiplatform.JvmOverloads

/**
 * Created by pdvrieze on 15/11/15.
 */
interface XmlReader : Closeable, Iterator<EventType> {

    /** Get the next tag. This must call next, not use the underlying stream.  */
    fun nextTag(): EventType {
        var event = next()
        while (event !== EventType.START_ELEMENT && event !== EventType.END_ELEMENT) {
            if (event === EventType.TEXT) {
                if (!isXmlWhitespace(text)) {
                    throw XmlException("Unexpected text content")
                }
            }
            event = next()
        }
        return event
    }

    override operator fun hasNext(): Boolean

    override operator fun next(): EventType

    val namespaceURI: String

    val localName: String

    val prefix: String

    val name: QName get() = qname(namespaceURI, localName, prefix)

    val isStarted: Boolean

    fun require(type: EventType, namespace: String?, name: String?): Unit = when {
        eventType !== type        ->
            throw XmlException("Unexpected event type Found: $eventType expected $type")

        namespace != null &&
        namespace != namespaceURI ->
            throw XmlException(
                "Namespace uri's don't match: expected=$namespace found=$namespaceURI")

        name != null &&
        name != localName         ->
            throw XmlException("Local names don't match: expected=$name found=$localName")

        else                      -> Unit
    }


    val depth: Int

    val text: String

    val attributeCount: Int

    fun getAttributeNamespace(index: Int): String

    fun getAttributePrefix(index: Int): String

    fun getAttributeLocalName(index: Int): String

    fun getAttributeName(index: Int): QName =
        qname(getAttributeNamespace(index), getAttributeLocalName(index),
                                    getAttributePrefix(index))

    fun getAttributeValue(index: Int): String

    val eventType: EventType

    fun getAttributeValue(nsUri: String?, localName: String): String?

    val namespaceStart: Int

    val namespaceEnd: Int

    fun getNamespacePrefix(index: Int): String

    override fun close()

    fun getNamespaceURI(index: Int): String

    fun getNamespacePrefix(namespaceUri: String): String?

    fun isWhitespace(): Boolean = eventType === EventType.IGNORABLE_WHITESPACE ||
                                  (eventType === EventType.TEXT &&
                                   isXmlWhitespace(text))

    fun isEndElement(): Boolean = eventType === EventType.END_ELEMENT

    /** Is the currrent element character content */
    fun isCharacters(): Boolean = eventType === EventType.TEXT

    /** Is the current element a start element */
    fun isStartElement(): Boolean = eventType === EventType.START_ELEMENT

    fun getNamespaceURI(prefix: String): String?

    /** Get some information on the current location in the file. This is implementation dependent.  */
    val locationInfo: String?

    /** The current namespace context */
    val namespaceContext: NamespaceContext

    val encoding: String?

    val standalone: Boolean?

    val version: String?
}

val XmlReader.attributes: Array<out XmlEvent.Attribute>
    get() =
        Array(attributeCount) { i ->
            XmlEvent.Attribute(locationInfo,
                               getAttributeNamespace(i),
                               getAttributeLocalName(i),
                               getAttributePrefix(i),
                               getAttributeValue(i))
        }

val XmlReader.namespaceIndices: IntRange get() = namespaceStart..(namespaceEnd - 1)

val XmlReader.attributeIndices: IntRange get() = 0..(attributeCount - 1)

val XmlReader.namespaceDecls: Array<out Namespace>
    get() =
        Array<Namespace>(namespaceEnd - namespaceStart) { i ->
            val nsIndex = namespaceStart + i
            XmlEvent.NamespaceImpl(getNamespacePrefix(nsIndex), getNamespaceURI(nsIndex))
        }

val XmlReader.qname: QName get() = text.toQname()

fun XmlReader.isPrefixDeclaredInElement(prefix: String): Boolean {
    val r = this
    for (i in r.namespaceStart until r.namespaceEnd) {
        if (r.getNamespacePrefix(i) == prefix) {
            return true
        }
    }
    return false
}

@JvmOverloads
fun XmlReader.unhandledEvent(message: String? = null) {
    val actualMessage = when (eventType) {
        EventType.CDSECT,
        EventType.TEXT          -> if (!isWhitespace()) message
                                                        ?: "Content found where not expected [$locationInfo] Text:'$text'" else null
        EventType.START_ELEMENT -> message ?: "Element found where not expected [$locationInfo]: $name"
        EventType.END_DOCUMENT  -> message ?: "End of document found where not expected"
        else                    -> null
    }// ignore

    actualMessage?.let { throw XmlException(it) }
}

fun XmlReader.isElement(elementname: QName): Boolean {
    return isElement(EventType.START_ELEMENT, elementname.getNamespaceURI(), elementname.getLocalPart(),
                     elementname.getPrefix())
}

/**
 * Get the next text sequence in the reader. This will skip over comments and ignorable whitespace, but not tags.
 * Any tags encountered with cause an exception to be thrown. It can either be invoked when in a start tag to return
 * all text content, or on a content element to include it (if text or cdata) and all subsequent siblings.
 *
 * The function will move to the containing end tag.
 *
 * @return   The text found
 *
 * @throws XmlException If reading breaks, or an unexpected element was found.
 */
fun XmlReader.allText(): String {
    val t = this
    return buildString {
        var type: EventType? = null
        if (eventType == EventType.TEXT || eventType == EventType.CDSECT) {
            append(text)
        }

        while ((t.next().apply { type = this@apply }) !== EventType.END_ELEMENT) {
            when (type) {
                EventType.COMMENT              -> {
                } // ignore
                EventType.IGNORABLE_WHITESPACE ->
                    // ignore whitespace starting the element.
                    if (length != 0) append(t.text)

                EventType.TEXT,
                EventType.CDSECT               -> append(t.text)
                else                           -> throw XmlException(
                    "Found unexpected child tag")
            }//ignore

        }

    }
}

fun XmlReader.skipElement() {
    val t = this
    t.require(EventType.START_ELEMENT, null, null)
    while (t.hasNext() && t.next() !== EventType.END_ELEMENT) {
        if (t.eventType === EventType.START_ELEMENT) {
            t.skipElement()
        }
    }
}

fun XmlReader.readSimpleElement(): String {
    val t = this
    t.require(EventType.START_ELEMENT, null, null)
    return buildString {

        while ((t.next()) !== EventType.END_ELEMENT) {
            when (t.eventType) {
                EventType.COMMENT,
                EventType.PROCESSING_INSTRUCTION -> {
                }
                EventType.TEXT,
                EventType.CDSECT                 -> append(t.text)
                else                             -> throw XmlException(
                    "Expected text content or end tag, found: ${t.eventType}")
            }/* Ignore */
        }

    }
}


/**
 * Skil the preamble events in the stream reader
 * @receiver The stream reader to skip
 */
fun XmlReader.skipPreamble() {
    while (isIgnorable() && hasNext()) {
        next()
    }
}

fun XmlReader.isIgnorable() = when (eventType) {
    EventType.COMMENT,
    EventType.START_DOCUMENT,
    EventType.END_DOCUMENT,
    EventType.PROCESSING_INSTRUCTION,
    EventType.DOCDECL,
    EventType.IGNORABLE_WHITESPACE -> true
    EventType.TEXT                 -> isXmlWhitespace(text)
    else                           -> false
}

/**
 * Check that the current state is a start element for the given name. The mPrefix is ignored.
 * @receiver The stream reader to check
 *
 * @param type The event type to check
 * @param elementname The name to check against  @return `true` if it matches, otherwise `false`
 */

fun XmlReader.isElement(type: EventType, elementname: QName): Boolean {
    return this.isElement(type, elementname.getNamespaceURI(), elementname.getLocalPart(), elementname.getPrefix())
}

/**
 * Check that the current state is a start element for the given name. The mPrefix is ignored.
 * @receiver The stream reader to check
 *
 * @param elementNamespace  The namespace to check against.
 *
 * @param elementName The local name to check against
 *
 * @param elementPrefix The mPrefix to fall back on if the namespace can't be determined
 *
 * @return `true` if it matches, otherwise `false`
 */
@JvmOverloads
fun XmlReader.isElement(elementNamespace: String?,
                                              elementName: String,
                                              elementPrefix: String? = null): Boolean {
    return this.isElement(EventType.START_ELEMENT, elementNamespace, elementName, elementPrefix)
}

/**
 * Check that the current state is a start element for the given name. The mPrefix is ignored.
 * @receiver The stream reader to check
 *
 * @param type The type to verify. Should be named so start or end element
 * @param elementNamespace  The namespace to check against.
 *
 * @param elementName The local name to check against
 *
 * @param elementPrefix The prefix to fall back on if the namespace can't be determined    @return `true` if it matches, otherwise `false`
 */
fun XmlReader.isElement(type: EventType,
                                              elementNamespace: String?,
                                              elementName: String,
                                              elementPrefix: String? = null): Boolean {
    val r = this
    if (r.eventType !== type) {
        return false
    }
    val expNs: CharSequence? = elementNamespace?.let { if (it.isEmpty()) null else it }

    if (r.localName != elementName) {
        return false
    }

    return when {
        !elementNamespace.isNullOrEmpty() -> expNs == r.namespaceURI
        elementPrefix.isNullOrEmpty()     -> r.prefix.isEmpty()
        else                              -> elementPrefix == r.prefix
    }
}

/**
 * Write the current event to the writer. This will **not** move the reader.
 */
fun XmlReader.writeCurrent(writer: XmlWriter) = eventType.writeEvent(writer, this)
