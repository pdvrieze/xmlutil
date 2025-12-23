/*
 * Copyright (c) 2024-2025.
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

package nl.adaptivity.xmlutil.core.impl

import nl.adaptivity.xmlutil.*

private const val COMMENT = "<!---->"

// Internal shared functionality between dom writer and stax writer
@XmlUtilInternal
public fun String.toIndentSequence(): List<XmlEvent.TextEvent> {
    val result = mutableListOf<XmlEvent.TextEvent>()
    val sb = StringBuilder()

    fun sbToTextEvent() {
        if (sb.isNotEmpty()) {
            val text = sb.toString()
            if (!isXmlWhitespace(text)) {
                throw XmlException("Indents can only be whitespace or comments: $text")
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

@XmlUtilInternal
public fun String.validateIndentString() {
    var commentPos = 0
    for (ch in this) {
        when (ch) {
            ' ', '\n', '\r', '\t' -> when (commentPos) {
                0, 4 -> {} // either before comment or inside comment
                5 -> commentPos = 4 // dash was not part of comment
                6 -> throw XmlException("-- is not allowed to occur inside xml comment text")
                else -> throw XmlException("Indent cannot contain non-comment text")
            }

            '<' if (commentPos == 0) -> ++commentPos

            '!' if (commentPos == 1) -> ++commentPos

            '-' -> when (commentPos) {
                2 -> ++commentPos
                3 -> ++commentPos // Now in comment

                4, 5 -> ++commentPos

                6 -> throw XmlException("-- is not allowed to occur inside xml comment text")
                else -> throw XmlException("Indent cannot contain non-comment text")
            }

            '>' -> when (commentPos) {
                6 -> commentPos = 0
                5 -> commentPos = 4
                4 -> {} // nothing

                else -> throw XmlException("Indent cannot contain non-comment text")
            }

            else if (commentPos != 4) -> throw XmlException("Indent cannot contain non-comment text: '$ch'")
        }
    }
    if (commentPos > 0) throw XmlException("Indent can not contain unclosed comment")
}

// Internal shared functionality between dom writer and stax writer
@XmlUtilInternal
public fun Iterable<XmlEvent.TextEvent>.toIndentString(): String = joinToString("") { ev ->
    when (ev.eventType) {
        EventType.COMMENT -> "<!--${ev.text}-->"

        else -> buildString {
            for (c in ev.text) {
                when (c) {
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '&' -> append("&amp;")
                    else -> append(c)
                }
            }
        }
    }
}


