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

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

import io.github.pdvrieze.xml.schematypes.values.XsdString
import io.github.pdvrieze.xml.schematypes.values.instances.XsdPrefixString
import io.github.pdvrieze.xml.schematypes.values.instances.XsdPrefixStringList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.xmlCollapseWhitespace

@Serializable
enum class WhitespaceValueCompat {
    @SerialName("preserve")
    PRESERVE {
        override fun normalize(representation: XsdString): XsdString = representation
        override fun canOverride(oldValue: WhitespaceValueCompat): Boolean = (oldValue == PRESERVE)
    },

    @SerialName("replace")
    REPLACE {

        private fun replace(representation: String): String {
            return buildString(representation.length, fun StringBuilder.() {
                for (c in representation) when (c) {
                    '\t', '\n', '\r' -> append(' ')
                    else -> append(c)
                }
            })
        }

        override fun normalize(representation: XsdString): XsdString = when(representation) {
            is XsdPrefixStringList -> XsdPrefixStringList(representation.elems.map { normalize(it) })
            is XsdPrefixString -> XsdPrefixString(
                namespace = replace(representation.namespace),
                prefix = replace(representation.prefix),
                localname = replace(representation.localname),
            )

            else -> XsdString(replace(representation.xmlString))
        }

        override fun canOverride(oldValue: WhitespaceValueCompat): Boolean = (oldValue != COLLAPSE)
    },

    @SerialName("collapse")
    COLLAPSE {
        override fun normalize(representation: XsdString): XsdString = when (representation) {
            is XsdPrefixStringList -> XsdPrefixStringList(
                representation.elems.asSequence()
                    .filter { it is XsdPrefixString || it.isNotEmpty() }
                    .map { normalize(it) }
                    .toList()
            )
            is XsdPrefixString -> XsdPrefixString(
                namespace = xmlCollapseWhitespace(representation.namespace),
                prefix = xmlCollapseWhitespace(representation.prefix),
                localname = xmlCollapseWhitespace(representation.localname),
            )
            else -> XsdString(xmlCollapseWhitespace(representation.xmlString))
        }

        override fun canOverride(oldValue: WhitespaceValueCompat): Boolean = true

    };

    abstract fun normalize(representation: XsdString): XsdString

    abstract fun canOverride(oldValue: WhitespaceValueCompat): Boolean

}
