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

package nl.adaptivity.xmlutil.serialization.impl

import nl.adaptivity.xmlutil.*

/**
 * Reader that is not actually buffering, but instead allows peekEvent (but then not giving access to the original)
 */
internal class PseudoBufferedReader(private val delegate: XmlReader) : XmlPeekingReader {
    override var hasPeekItems = false
        private set

    override val version: String? get() = ifNotPeeking { version }
    override val standalone: Boolean? get() = ifNotPeeking { standalone }
    override val encoding: String? get() = ifNotPeeking { encoding }
    override val eventType: EventType get() = ifNotPeeking { eventType }
    override val attributeCount: Int get() = ifNotPeeking { attributeCount }
    override val piData: String get() = ifNotPeeking { piData }
    override val piTarget: String get() = ifNotPeeking { piTarget }
    override val text: String get() = ifNotPeeking { text }
    override val isStarted: Boolean get() = if (hasPeekItems) delegate.eventType != EventType.START_DOCUMENT else delegate.isStarted
    override val prefix: String get() = ifNotPeeking { prefix }
    override val localName: String get() = ifNotPeeking { localName }
    override val namespaceURI: String get() = ifNotPeeking { namespaceURI }
    override val depth: Int
        get() = when {
            hasPeekItems -> when (delegate.eventType) {
                EventType.START_ELEMENT -> delegate.depth - 1
//                EventType.END_ELEMENT -> delegate.depth + 1
                else -> delegate.depth
            }

            else -> delegate.depth
        }

    @Suppress("DEPRECATION")
    @Deprecated(
        "Use extLocationInfo as that allows more detailed information",
        replaceWith = ReplaceWith("extLocationInfo?.toString()")
    )
    override val locationInfo: String? get() = ifNotPeeking { locationInfo }

    override fun hasNext(): Boolean = hasPeekItems || delegate.hasNext()

    override fun next(): EventType {
        if (hasPeekItems) {
            hasPeekItems = false;
            return delegate.eventType
        }
        return delegate.next()
    }

    override fun peekNextEvent(): EventType? {
        if (hasPeekItems) return delegate.eventType
        hasPeekItems = true
        return if (delegate.hasNext()) delegate.next() else null
    }

    override fun pushBackCurrent() = ifNotPeeking {
        hasPeekItems = true
    }

    override fun getAttributeNamespace(index: Int): String = ifNotPeeking {
        getAttributeNamespace(index)
    }

    override fun getAttributePrefix(index: Int): String = ifNotPeeking {
        getAttributePrefix(index)
    }

    override fun getAttributeLocalName(index: Int): String = ifNotPeeking {
        getAttributeLocalName(index)
    }

    override fun getAttributeValue(index: Int): String = ifNotPeeking {
        getAttributeValue(index)
    }

    override fun getAttributeValue(nsUri: String?, localName: String): String? = ifNotPeeking {
        getAttributeValue(nsUri, localName)
    }

    override fun getNamespacePrefix(namespaceUri: String): String? = ifNotPeeking {
        getNamespacePrefix(namespaceUri)
    }

    override fun close() {
        close()
    }

    override fun getNamespaceURI(prefix: String): String? = ifNotPeeking {
        getNamespaceURI(prefix)
    }

    override val namespaceDecls: List<Namespace> get() = ifNotPeeking {
        namespaceDecls
    }

    override val namespaceContext: IterableNamespaceContext
        get() = delegate.namespaceContext

    private inline fun <T> ifNotPeeking(action: XmlReader.() -> T): T {
        check (!hasPeekItems) {
            "Attempting to read state in peeking mode"
        }
        return delegate.action()
    }
}

