/*
 * Copyright (c) 2019.
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

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.XmlEvent.*
import nl.adaptivity.xmlutil.core.impl.NamespaceHolder


@XmlUtilInternal
public abstract class XmlBufferedReaderBase(private val delegate: XmlReader) : XmlReader {
    private val namespaceHolder = NamespaceHolder()

    init { // Record also for the first element
        if (delegate.isStarted) {
            for (ns in delegate.namespaceContext) {
                namespaceHolder.addPrefixToContext(ns)
            }
        }
    }

    @XmlUtilInternal
    protected abstract val hasPeekItems: Boolean

    @XmlUtilInternal
    protected var current: XmlEvent? = if (delegate.isStarted) XmlEvent.from(delegate) else null
        private set

    private val currentElement: StartElementEvent
        get() = current as? StartElementEvent
            ?: throw XmlException("Expected a start element, but did not find it.")

    override val namespaceURI: String
        get() = when (current?.eventType) {
            EventType.ATTRIBUTE -> (current as Attribute).namespaceUri
            EventType.START_ELEMENT -> (current as StartElementEvent).namespaceUri
            EventType.END_ELEMENT -> (current as EndElementEvent).namespaceUri
            else -> throw XmlException("Attribute not defined here: namespaceUri")
        }


    override val localName: String
        get() = when (current?.eventType) {
            EventType.ENTITY_REF -> (current as EntityRefEvent).localName
            EventType.ATTRIBUTE -> (current as Attribute).localName
            EventType.START_ELEMENT -> (current as StartElementEvent).localName
            EventType.END_ELEMENT -> (current as EndElementEvent).localName
            else -> throw XmlException(
                "Attribute not defined here: localName"
            )
        }

    override val prefix: String
        get() = when (current?.eventType) {
            EventType.ATTRIBUTE -> (current as Attribute).prefix
            EventType.START_ELEMENT -> (current as StartElementEvent).prefix
            EventType.END_ELEMENT -> (current as EndElementEvent).prefix
            else -> throw XmlException("Attribute not defined here: prefix")
        }

    override val depth: Int
        get() = namespaceHolder.depth

    protected fun incDepth() { namespaceHolder.incDepth() }
    protected fun decDepth() { namespaceHolder.decDepth() }

    override val text: String
        get() {
            return if (current!!.eventType === EventType.ATTRIBUTE) {
                (current as Attribute).value
            } else (current as TextEvent).text
        }

    override val attributeCount: Int
        get() = currentElement.attributes.size

    override val isStarted: Boolean
        get() = current != null

    override val eventType: EventType
        get() = current?.eventType ?: if (hasNext()) {
            throw XmlException("Attempting to get the event type before getting an event.")
        } else {
            throw XmlException("Attempting to read beyond the end of the stream")
        }

    override val locationInfo: String?
        get() { // allow for location info at the start of the document
            return current?.locationInfo ?: delegate.locationInfo
        }

    override val namespaceContext: IterableNamespaceContext
        get() {
            return when (val c = current) {
                is StartElementEvent -> c.namespaceContext
                is EndElementEvent -> c.namespaceContext
                else -> namespaceHolder.namespaceContext
                    //delegate.namespaceContext // We are not in a place that could introduce more, so use the delegate directly
            }
        }

    override val namespaceDecls: List<Namespace>
        get() = when (val c = current) {
            is StartElementEvent -> c.namespaceDecls.toList()
            else -> namespaceHolder.namespacesAtCurrentDepth
        }

    override val encoding: String?
        get() = (current as StartDocumentEvent).encoding

    override val standalone: Boolean?
        get() = (current as StartDocumentEvent).standalone

    override val version: String?
        get() = (current as StartDocumentEvent).version

    public fun nextEvent(): XmlEvent {
        if (hasPeekItems) {
            return removeFirstToCurrent()
        }
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        peek()
        return removeFirstToCurrent()
    }

    private fun removeFirstToCurrent(): XmlEvent {
        val event: XmlEvent = bufferRemoveFirst()
        this.current = event

        when (event.eventType) {
            EventType.START_ELEMENT -> {
                namespaceHolder.incDepth()
                val start = event as StartElementEvent
                for (ns in start.namespaceDecls) {
                    namespaceHolder.addPrefixToContext(ns)
                }
            }
            EventType.END_ELEMENT -> namespaceHolder.decDepth()
            else -> {
            } /* Do nothing */
        }
        return event
    }

    /**
     * Try to peek the next event. Unlike [peekFirst] this function will progress the underlying stream if needed.
     */
    public fun peek(): XmlEvent? {
        if (!hasPeekItems) {
            addAll(doPeek())
        }
        return peekFirst()
    }

    /**
     * Put the current element in the peek buffer. This is basically a very limited pushback
     */
    public abstract fun pushBackCurrent()

    /**
     * Get the next event to add to the queue. Children can override this to customize the events that are added to the
     * peek buffer. Normally this method is only called when the peek buffer is empty.
     */
    @XmlUtilInternal
    protected open fun doPeek(): List<XmlEvent> {
        if (delegate.hasNext()) {
            delegate.next() // Don't forget to actually read the next element
            val event = XmlEvent.from(delegate)
            val result = ArrayList<XmlEvent>(1)
            result.add(event)
            return result
        }
        return emptyList()
    }

    final override fun hasNext(): Boolean {
        if (hasPeekItems) {
            return true
        }
        return peek() != null

    }

    @XmlUtilInternal
    protected fun stripWhiteSpaceFromPeekBuffer() {
        while (hasPeekItems && peekLast().let { peekLast ->
                peekLast is TextEvent && isXmlWhitespace(peekLast.text)
            }) {
            bufferRemoveLast()
        }
    }


    protected abstract fun peekFirst(): XmlEvent?

    @XmlUtilInternal
    protected abstract fun peekLast(): XmlEvent?

    @XmlUtilInternal
    protected abstract fun bufferRemoveLast(): XmlEvent

    @XmlUtilInternal
    protected abstract fun bufferRemoveFirst(): XmlEvent

    @XmlUtilInternal
    protected abstract fun add(event: XmlEvent)

    @XmlUtilInternal
    protected abstract fun addAll(events: Collection<XmlEvent>)

    override fun close() {
        delegate.close()
    }

    override fun nextTag(): EventType {
        return nextTagEvent().eventType
    }

    public fun nextTagEvent(): XmlEvent {
        val current = nextEvent()
        return when (current.eventType) {
            EventType.TEXT -> {
                if (isXmlWhitespace((current as TextEvent).text)) {
                    nextTagEvent()
                } else {
                    throw XmlException("Unexpected element found when looking for tags: $current")
                }
            }
            EventType.START_DOCUMENT,
            EventType.COMMENT, EventType.IGNORABLE_WHITESPACE,
            EventType.PROCESSING_INSTRUCTION -> nextTagEvent()
            EventType.START_ELEMENT, EventType.END_ELEMENT -> current
            else -> throw XmlException(
                "Unexpected element found when looking for tags: $current"
            )
        }
    }

    override fun next(): EventType {
        return nextEvent().eventType
    }

    override fun getAttributeNamespace(index: Int): String = currentElement.attributes[index].namespaceUri

    override fun getAttributePrefix(index: Int): String = currentElement.attributes[index].prefix

    override fun getAttributeLocalName(index: Int): String = currentElement.attributes[index].localName

    override fun getAttributeValue(index: Int): String = currentElement.attributes[index].value

    override fun getAttributeValue(nsUri: String?, localName: String): String? =
        currentElement.attributes.firstOrNull { attr ->
            (nsUri == null || nsUri == attr.namespaceUri) && localName == attr.localName
        }?.value

    override fun getNamespacePrefix(namespaceUri: String): String? {
        return currentElement.getPrefix(namespaceUri)
    }

    override fun getNamespaceURI(prefix: String): String? {
        return currentElement.getNamespaceURI(prefix)
    }
}
