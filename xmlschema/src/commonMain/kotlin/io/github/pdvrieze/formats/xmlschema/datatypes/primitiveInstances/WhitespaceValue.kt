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

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.xmlCollapseWhitespace

@Serializable
enum class WhitespaceValue {
    @SerialName("preserve")
    PRESERVE {
        override fun normalize(representation: VString): VString = representation
        override fun canOverride(oldValue: WhitespaceValue): Boolean = (oldValue == PRESERVE)
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

        override fun normalize(representation: VString): VString = when(representation) {
            is VPrefixStringList -> VPrefixStringList(representation.elems.map { normalize(it) })
            is VPrefixString -> VPrefixString(
                namespace = replace(representation.namespace),
                prefix = replace(representation.prefix),
                localname = replace(representation.localname),
            )

            else -> VString(replace(representation.xmlString))
        }

        override fun canOverride(oldValue: WhitespaceValue): Boolean = (oldValue != COLLAPSE)
    },

    @SerialName("collapse")
    COLLAPSE {
        override fun normalize(representation: VString): VString = when (representation) {
            is VPrefixStringList -> VPrefixStringList(representation.elems.asSequence()
                .filter { it is VPrefixString || it.isNotEmpty() }
                .map { normalize(it) }
                .toList()
            )
            is VPrefixString -> VPrefixString(
                namespace = xmlCollapseWhitespace(representation.namespace),
                prefix = xmlCollapseWhitespace(representation.prefix),
                localname = xmlCollapseWhitespace(representation.localname),
            )
            else -> VString(xmlCollapseWhitespace(representation.xmlString))
        }

        override fun canOverride(oldValue: WhitespaceValue): Boolean = true

    };

    abstract fun normalize(representation: VString): VString

    abstract fun canOverride(oldValue: WhitespaceValue): Boolean

}
