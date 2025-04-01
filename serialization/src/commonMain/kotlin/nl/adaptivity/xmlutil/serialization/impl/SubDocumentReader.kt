/*
 * Copyright (c) 2025.
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

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlPeekingReader

internal class SubDocumentReader(
    val delegate: XmlPeekingReader,
    val isParseAllSiblings: Boolean
) : XmlPeekingReader by delegate {
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

    override fun next(): EventType = when {
        started -> {
            if ((initialDepth < 0) || delegate.depth <= initialDepth && !hasNext()) {
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

            isParseAllSiblings -> delegate.depth > initialDepth || delegate.peekNextEvent() != EventType.END_ELEMENT

            else -> delegate.eventType != EventType.END_ELEMENT || delegate.depth > initialDepth

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
        while (hasNext()) next()
    }
}
