/*
 * Copyright (c) 2025.
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

@file:MustUseReturnValues

package nl.adaptivity.xmlutil.serialization.impl

import nl.adaptivity.xmlutil.*

internal class SubDocumentReader(
    val delegate: XmlPeekingReader,
    val isParseAllSiblings: Boolean
) : XmlPeekingReader {
    private var initialDepth = when (delegate.eventType) {
        EventType.START_ELEMENT -> when {
            isParseAllSiblings -> delegate.depth -1 // we parse all siblings of the tag (excluding its end tag)
            else -> delegate.depth
        }

        else -> when {
            isParseAllSiblings -> delegate.depth
            else -> -1
        }
    }

    private var started = false

    override val isStarted: Boolean
        get() = started && delegate.isStarted

    override val attributeCount: Int get() = checkInValidState { delegate.attributeCount }
    override val piData: String get() = checkInValidState { delegate.piData }
    override val piTarget: String get() = checkInValidState { delegate.piTarget }
    override val text: String get() = checkInValidState { delegate.text }
    override val isKnownEntity: Boolean get() = checkInValidState { delegate.isKnownEntity }
    override val prefix: String get() = checkInValidState { delegate.prefix }
    override val localName: String get() = checkInValidState { delegate.localName }
    override val namespaceURI: String get() = checkInValidState { delegate.namespaceURI }
    override val eventType: EventType get() = checkInValidState { delegate.eventType }
    override val namespaceDecls: List<Namespace> get() = checkInValidState { delegate.namespaceDecls }
    override val extLocationInfo: XmlReader.LocationInfo? get() = checkInValidState { delegate.extLocationInfo }
    override val namespaceContext: IterableNamespaceContext get() = checkInValidState { delegate.namespaceContext }
    override val encoding: String? get() = checkInValidState { delegate.encoding }
    override val standalone: Boolean? get() = checkInValidState { delegate.standalone }
    override val version: String? get() = checkInValidState { delegate.version }

    override fun next(): EventType = when {
        started -> {
            if ((initialDepth < 0) || (delegate.depth <= initialDepth && !hasNext())) {
                throw IllegalStateException("Reading beyond end of subdocument reader")
            }

            delegate.next()
        }

        else -> {
            started = true
            delegate.eventType
        }
    }

    override fun nextTag(): EventType = when {
        started -> super.nextTag()
        else -> when (delegate.eventType) {
            EventType.START_ELEMENT,
            EventType.END_ELEMENT -> {
                started = true
                delegate.eventType
            }

            else -> {
                started = true
                super.nextTag()
            }
        }
    }

    override fun pushBackCurrent() = when {
        !started -> throw IllegalStateException("Sub reader has not started yet")
        initialDepth < 0 -> started = false // single non-element content just reset the reader
        else -> delegate.pushBackCurrent()
    }

    override val hasPeekItems: Boolean
        get() = !started || delegate.hasPeekItems

    override fun peekNextEvent(): EventType? = when {
        started -> delegate.peekNextEvent()
        else -> delegate.eventType
    }

    override fun hasNext(): Boolean {
        return when {
            !started -> true

            initialDepth < 0 -> false

            delegate.depth > initialDepth -> true

            isParseAllSiblings -> delegate.peekNextEvent() != EventType.END_ELEMENT

            else -> delegate.eventType != EventType.END_ELEMENT

            /*
                        initialDepth < 0 -> false

                        delegate.depth == initialDepth &&
                                !isParseAllSiblings &&
                                delegate.eventType == EventType.END_ELEMENT
                                     -> false

                        delegate.peekNextEvent() == EventType.END_ELEMENT -> {
                            delegate.depth < initialDepth
                        }

                        else -> true
            */
        }
    }


    override val depth: Int
        get() = delegate.depth - initialDepth

    override fun close() {
        while (hasNext()) {
            val _ = next()
        }
    }

    override fun toString(): String {
        return buildString {
            append("<SubDocumentReader>(")
            when {
                !started -> append("Not started - ${delegate.toEvent()}")
                initialDepth < 0 -> append("Finished")
                else -> append(delegate.toEvent())
            }
            append(')')
        }
    }

    private inline fun <R> checkInValidState(body: () -> R): R = when {
        ! started -> throw IllegalStateException("The sub reader has not started yet")
        delegate.hasPeekItems -> return body() // may fail in delegate, but we are not beyond the reader
        initialDepth >= 0 && initialDepth > delegate.depth -> throw IllegalStateException("Sub reader is finished")
        else -> return body()
    }

    override fun getAttributeNamespace(index: Int): String = checkInValidState {
        return delegate.getAttributeNamespace(index)
    }

    override fun getAttributePrefix(index: Int): String = checkInValidState {
        return delegate.getAttributePrefix(index)
    }

    override fun getAttributeLocalName(index: Int): String = checkInValidState {
        return delegate.getAttributeLocalName(index)
    }

    override fun getAttributeValue(index: Int): String = checkInValidState {
        return delegate.getAttributeValue(index)
    }

    override fun getAttributeValue(nsUri: String?, localName: String): String? = checkInValidState {
        return delegate.getAttributeValue(nsUri, localName)
    }

    override fun getNamespacePrefix(namespaceUri: String): String? = checkInValidState {
        return delegate.getNamespacePrefix(namespaceUri)
    }

    override fun getNamespaceURI(prefix: String): String? = checkInValidState {
        return delegate.getNamespaceURI(prefix)
    }
}
