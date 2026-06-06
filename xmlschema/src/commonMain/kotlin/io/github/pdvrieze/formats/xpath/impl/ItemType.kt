/*
 * Copyright (c) 2023-2026.
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

package io.github.pdvrieze.formats.xpath.impl

import nl.adaptivity.xmlutil.*

interface ItemType {

    object ItemTest: ItemType {
        override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
            builder.append(toString())
        }

        override fun toString(): String = "item()"
    }

    class AtomicType(val name: QName): ItemType {
        override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
            if (output == null) {
                builder.append(name.toCName())
                return
            } else if (name.namespaceURI.isEmpty()) {
                builder.append(name.localPart)
            } else {
                val effectivePrefix = output.getOrCreatePrefix(name.namespaceURI, name.getPrefix())
                when (effectivePrefix) {
                    "" -> builder.append(name.localPart)
                    else -> builder.append(effectivePrefix).append(':').append(name.localPart)
                }
            }
        }

        override fun toString(): String = name.toCName()
    }

    fun appendToString(builder: StringBuilder, output: XmlWriter?)
}

sealed class SequenceType {
    object EmptySequence : SequenceType() {
        override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
            builder.append("empty-sequence()")
        }
    }

    class ItemSequence(val itemType: ItemType, val occurrence: OccurrenceType) : SequenceType() {
        override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
            itemType.appendToString(builder, output)
            builder.append(occurrence.literal)
        }
    }

    enum class OccurrenceType(val literal: String) {
        SINGLE(""),
        OPTIONAL("?"),
        ANY("*"),
        AT_LEAST_ONE("+")
    }

    abstract fun appendToString(builder: StringBuilder, output: XmlWriter?)
}
