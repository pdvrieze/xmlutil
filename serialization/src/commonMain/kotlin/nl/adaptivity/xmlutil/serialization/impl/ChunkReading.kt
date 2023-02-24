/*
 * Copyright (c) 2023.
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


private const val MAXCHUNKSIZE = 16384

internal fun consumeChunksFromString(str: String, consumeChunk: (chunk: String) -> Unit) {
    var startIdx = 0
    var endIdx = startIdx + MAXCHUNKSIZE
    while (startIdx + MAXCHUNKSIZE < str.length) {
        consumeChunk(str.substring(startIdx, endIdx)) // consume all but the last chunk
        startIdx = endIdx
        endIdx = startIdx + MAXCHUNKSIZE
    }
    consumeChunk(str.substring(startIdx)) // consume the last chunk
}

/**
 * From a start tag read the text only content of the element. Comments are allowed and handled, but subtags are not
 * allowed. This tag finishes at the end of the element.
 */
internal fun XmlReader.readSimpleElementChunked(consumeChunk: (chunk: String) -> Unit) {
    val t = this
    t.require(EventType.START_ELEMENT, null, null)
    while ((t.next()) !== EventType.END_ELEMENT) {
        when (t.eventType) {
            EventType.COMMENT,
            EventType.PROCESSING_INSTRUCTION -> {
            }
            EventType.IGNORABLE_WHITESPACE,
            EventType.TEXT,
            EventType.ENTITY_REF,
            EventType.CDSECT -> consumeChunksFromString(t.text, consumeChunk)
            else -> throw XmlException(
                "Expected text content or end tag, found: ${t.eventType}"
            )
        }/* Ignore */
    }
}

internal fun XmlBufferedReader.allConsecutiveTextContentChunked(consumeChunk: (chunk: String) -> Unit) {
    val t = this
    if (eventType.isTextElement || eventType == EventType.IGNORABLE_WHITESPACE) consumeChunksFromString(text, consumeChunk)

    var event: XmlEvent? = null

    loop@ while ((t.peek().apply { event = this@apply })?.eventType !== EventType.END_ELEMENT) {
        when (event?.eventType) {
            EventType.PROCESSING_INSTRUCTION,
            EventType.COMMENT
            -> {
                t.next();Unit
            } // ignore

            // ignore whitespace starting the element.
            EventType.IGNORABLE_WHITESPACE,
            EventType.TEXT,
            EventType.ENTITY_REF,
            EventType.CDSECT
            -> {
                t.next()
                consumeChunksFromString(t.text, consumeChunk)
            }
            EventType.START_ELEMENT
            -> {
                // don't progress the event either
                break@loop
            }

            else -> throw XmlException("Found unexpected child tag: $event")
        }//ignore

    }
}

internal fun XmlReader.allTextChunked(consumeChunk: (chunk: String) -> Unit) {
    val t = this
    var writtenChunk = false
    if (eventType.isTextElement && text.isNotEmpty()) {
        writtenChunk = true
        consumeChunksFromString(text, consumeChunk)
    }

    var type: EventType?

    while ((t.next().apply { type = this@apply }) !== EventType.END_ELEMENT) {
        when (type) {
            EventType.PROCESSING_INSTRUCTION,
            EventType.COMMENT -> Unit // ignore

            // ignore whitespace starting the element.
            EventType.IGNORABLE_WHITESPACE -> {
                if (writtenChunk) consumeChunksFromString(t.text, consumeChunk)
            }
            EventType.ENTITY_REF,
            EventType.TEXT,
            EventType.CDSECT -> {
                if (t.text.isNotEmpty()) {
                    writtenChunk = true
                    consumeChunksFromString(t.text, consumeChunk)
                }
            }

            else -> throw XmlException("Found unexpected child tag with type: $type")
        }//ignore

    }
    // Ensure at least one chunk
    if (!writtenChunk) consumeChunk("")
}

