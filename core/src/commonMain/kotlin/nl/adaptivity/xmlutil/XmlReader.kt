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

@file:JvmMultifileClass
@file:JvmName("XmlReaderUtil")

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.impl.multiplatform.Closeable
import nl.adaptivity.xmlutil.core.impl.multiplatform.Throws
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

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

    @Throws(XmlException::class)
    fun require(type: EventType, namespace: String?, name: String?) {
        when {
            eventType != type                 ->
                throw XmlException("Type $eventType does not match expected type \"$type\"")

            namespace != null &&
                    namespaceURI != namespace ->
                throw XmlException("Namespace ${namespaceURI} does not match expected \"$namespace\"")

            name != null &&
                    localName != name         ->
                throw XmlException("local name ${localName} does not match expected \"$name\"")
        }
    }

    @Throws(XmlException::class)
    fun require(type: EventType, name: QName?) {
        return require(type, name?.namespaceURI, name?.localPart)
    }


    val depth: Int

    val text: String

    val attributeCount: Int

    fun getAttributeNamespace(index: Int): String

    fun getAttributePrefix(index: Int): String

    fun getAttributeLocalName(index: Int): String

    fun getAttributeName(index: Int): QName =
        qname(
            getAttributeNamespace(index), getAttributeLocalName(index),
            getAttributePrefix(index)
             )

    fun getAttributeValue(index: Int): String

    val eventType: EventType

    fun getAttributeValue(nsUri: String?, localName: String): String?

    /**
     * Index of the first namespace relevant at this level. This allows the underlying implementation
     * to implement namespaces as a stack.
     */
    val namespaceStart: Int

    /**
     * Index of the last namespace relevant at this level
     */
    val namespaceEnd: Int

    /**
     * Namespace prefix at the given index
     */
    fun getNamespacePrefix(index: Int): String

    /**
     * Namespace uri at the given index
     */
    fun getNamespaceURI(index: Int): String

    fun getNamespacePrefix(namespaceUri: String): String?

    override fun close()

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
    val namespaceContext: FreezableNamespaceContext

    val encoding: String?

    val standalone: Boolean?

    val version: String?
}

val XmlReader.attributes: Array<out XmlEvent.Attribute>
    get() =
        Array(attributeCount) { i ->
            XmlEvent.Attribute(
                locationInfo,
                getAttributeNamespace(i),
                getAttributeLocalName(i),
                getAttributePrefix(i),
                getAttributeValue(i)
                              )
        }

val XmlReader.namespaceIndices: IntRange get() = namespaceStart until namespaceEnd

val XmlReader.attributeIndices: IntRange get() = 0 until attributeCount

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

fun XmlReader.isElement(elementname: QName): Boolean {
    return isElement(
        EventType.START_ELEMENT, elementname.getNamespaceURI(), elementname.getLocalPart(),
        elementname.getPrefix()
                    )
}

/**
 * Get the next text sequence in the reader. This will skip over comments and ignorable whitespace (starting the
 * content), but not tags. Any tags encountered with cause an exception to be thrown. It can either be invoked when in a
 * start tag to return all text content, or on a content element to include it (if text or cdata) and all subsequent
 * siblings.
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
        if (eventType.isTextElement) {
            append(text)
        }

        var type: EventType?

        while ((t.next().apply { type = this@apply }) !== EventType.END_ELEMENT) {
            when (type) {
                EventType.PROCESSING_INSTRUCTION,
                EventType.COMMENT
                     -> Unit // ignore

                // ignore whitespace starting the element.
                EventType.IGNORABLE_WHITESPACE
                     -> if (length != 0) append(t.text)

                EventType.ENTITY_REF,
                EventType.TEXT,
                EventType.CDSECT
                     -> append(t.text)

                else -> throw XmlException("Found unexpected child tag with type: $type")
            }//ignore

        }

    }
}

/**
 * Consume all text and non-content (comment/processing instruction) to get an uninterrupted text sequence. This will
 * skip over comments and ignorable whitespace that starts the string, but not tags. Any tags encountered will lead
 * to a return of this function.
 * Any tags encountered with cause an exception to be thrown. It can either be invoked when in a start tag to return
 * all text content, or on a content element to include it (if text or cdata) and all subsequent siblings.
 *
 * The function will move to the containing end tag.
 *
 * @return   The text found
 *
 * @throws XmlException If reading breaks, or an unexpected element was found.
 */
fun XmlBufferedReader.consecutiveTextContent(): String {
    val whiteSpace = StringBuilder()
    val t = this
    return buildString {
        if (eventType.isTextElement) {
            append(text)
        }

        var event: XmlEvent? = null

        loop@while ((t.peek().apply { event = this@apply })?.eventType !== EventType.END_ELEMENT) {
            when (event?.eventType) {
                EventType.PROCESSING_INSTRUCTION,
                EventType.COMMENT
                     -> { t.next();Unit } // ignore

                // ignore whitespace starting the element.
                EventType.IGNORABLE_WHITESPACE
                     -> { t.next(); whiteSpace.append(t.text) }

                EventType.TEXT,
                EventType.ENTITY_REF,
                EventType.CDSECT
                     -> {
                    t.next()
                    if (isNotEmpty()) {
                        append(whiteSpace)
                        whiteSpace.clear()
                    }
                    append(t.text)
                }
                EventType.START_ELEMENT
                     -> { // don't progress the event either
                    break@loop
                }

                else -> throw XmlException("Found unexpected child tag: $event")
            }//ignore

        }

    }
}

/**
 * From a start element, skip all element content until the corresponding end element has been read. After
 * invocation the end element has just been read (and would be returned on relevant state calls).
 */
fun XmlReader.skipElement() {
    val t = this
    t.require(EventType.START_ELEMENT, null, null)
    while (t.hasNext() && t.next() !== EventType.END_ELEMENT) {
        if (t.eventType === EventType.START_ELEMENT) {
            t.skipElement()
        }
    }
}

/**
 * From a start tag read the text only content of the element. Comments are allowed and handled, but subtags are not
 * allowed. This tag finishes at the end of the element.
 */
fun XmlReader.readSimpleElement(): String {
    val t = this
    t.require(EventType.START_ELEMENT, null, null)
    return buildString {

        while ((t.next()) !== EventType.END_ELEMENT) {
            when (t.eventType) {
                EventType.COMMENT,
                EventType.IGNORABLE_WHITESPACE,
                EventType.PROCESSING_INSTRUCTION -> {
                }
                EventType.TEXT,
                EventType.ENTITY_REF,
                EventType.CDSECT                 -> append(t.text)
                else                             -> throw XmlException(
                    "Expected text content or end tag, found: ${t.eventType}"
                                                                      )
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
fun XmlReader.isElement(
    elementNamespace: String?,
    elementName: String,
    elementPrefix: String? = null
                       ): Boolean {
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
fun XmlReader.isElement(
    type: EventType,
    elementNamespace: String?,
    elementName: String,
    elementPrefix: String? = null
                       ): Boolean {
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
