/*
 * Copyright (c) 2020.
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

package nl.adaptivity.xmlutil.core.impl

import nl.adaptivity.xmlutil.*

/**
 * Base class for platform xml writers. It contains common code. */
@XmlUtilInternal
public abstract class PlatformXmlWriterBase(indentSequence: Iterable<XmlEvent.TextEvent> = emptyList()) : XmlWriter {
    internal var indentSequence: List<XmlEvent.TextEvent> = indentSequence.toList()

    final override var indentString: String
        @Deprecated("Use indentSequence", level = DeprecationLevel.ERROR)
        get() {
            return indentSequence.joinToString { ev ->
                when (ev.eventType) {
                    EventType.COMMENT -> "<!--${ev.text}-->"
                    else -> ev.text
                }
            }
        }
        set(value) {
            indentSequence = value.toIndentSequence()
        }

    @get:Suppress("OverridingDeprecatedMember")
    override var indent: Int
        get() = indentSequence.sumOf {
            when (it.eventType) {
                EventType.COMMENT -> 7 + it.text.length
                else -> it.text.length
            }
        }
        set(value) {
            indentSequence = listOf(XmlEvent.TextEvent(null, EventType.IGNORABLE_WHITESPACE, " ".repeat(value)))
        }

    internal companion object {
        private const val COMMENT = "<!---->"

        internal fun String.toIndentSequence(): List<XmlEvent.TextEvent> {
            val result = mutableListOf<XmlEvent.TextEvent>()
            val sb = StringBuilder()

            fun sbToTextEvent() {
                if (sb.isNotEmpty()) {
                    val text = sb.toString()
                    if (!text.isXmlWhitespace()) {
                        throw XmlException("Indents can only be whitespace or comments: ${text}")
                    }
                    result.add(XmlEvent.TextEvent(null, EventType.IGNORABLE_WHITESPACE, text))
                    sb.clear()
                }
            }

            var commentPos = 0
            for (ch in this) {
                when (ch) {
                    '<' -> when (commentPos) {
                        0 -> ++commentPos
                        else -> sb.append(ch)
                    }
                    '!' -> when (commentPos) {
                        1 -> ++commentPos
                        else -> sb.append(ch)
                    }
                    '-' -> when (commentPos) {
                        2 -> ++commentPos
                        3 -> { // Now in comment
                            ++commentPos
                            sbToTextEvent()
                        }
                        4, 5 -> ++commentPos
                        6 -> throw XmlException("-- is not allowed to occur inside xml comment text")
                        else -> sb.append(ch)
                    }
                    '>' -> when (commentPos) {
                        6 -> {
                            commentPos = 0
                            result.add(XmlEvent.TextEvent(null, EventType.COMMENT, sb.toString()))
                            sb.clear()
                        }
                        5 -> {
                            commentPos = 4
                            sb.append("->")
                        }
                        else -> sb.append(ch)
                    }
                    else -> when (commentPos) {
                        1, 2, 3 -> { // Reset comment position, add string
                            sb.append(COMMENT, 0, commentPos)
                            commentPos = 0
                            sb.append(ch)
                        }
                        0, 4 -> sb.append(ch) // Not in comment transition, just append
                        5 -> { // single - in comment
                            commentPos = 4
                            sb.append('-').append(ch)
                        }
                        6 -> throw XmlException("-- is not allowed to occur inside xml comment text")
                    }
                }
            }
            if (commentPos > 0) throw XmlException("Indent can not contain unclosed comment")
            sbToTextEvent()
            return result
        }
    }
}
