/*
 * Copyright (c) 2026.
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

package nl.adaptivity.xmlutil.core

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.core.impl.EntityMap
import nl.adaptivity.xmlutil.core.internal.DEBUG
import nl.adaptivity.xmlutil.core.internal.addDigitToCodePoint
import nl.adaptivity.xmlutil.core.internal.appendCodepoint

@ExperimentalXmlUtilApi
public open class XmlEntity(
    public val replacementValue: CharSequence,
    isSimple: Boolean,
    public val location: XmlReader.LocationInfo? = null
) {

    init {
        if (DEBUG && isSimple) {
            require(replacementValue.none { it == '<' || it == '>' || it == '&' }) {
                "Invalid character found in replacement value for simple entity: $replacementValue"
            }
        }
    }

    // This is secondary so that init can use the parameter for checking. Even if this property
    // is overridden by the default implementations.
    @Suppress("CanBePrimaryConstructorProperty")
    public open val isSimple: Boolean = isSimple

    public open val simpleValue: CharSequence get() = replacementValue

    public fun resolveEmbeddedEntities(entityMap: EntityMap): String {
        return buildString {
            var i = 0
            val e = replacementValue.length
            while (i < e) {
                when (replacementValue[i]) {
                    '&' -> {
                        if (replacementValue[i + 1] == '#') {
                            val isHex = when (replacementValue[i + 2]) {
                                'x' -> {
                                    i += 3
                                    true
                                }

                                else -> {
                                    i += 2
                                    false
                                }
                            }
                            var current = 0
                            while (replacementValue[i] != ';') {
                                current = addDigitToCodePoint(replacementValue[i], isHex, current)
                                i += 1
                            }
                            appendCodepoint(current)
                            // i points at ';'; outer i += 1 below advances past it
                        } else {
                            i += 1 // skip '&', i now points at start of entity name
                            val end = replacementValue.indexOf(';', i)
                            val name = replacementValue.substring(i, end)
                            i = end // i points at ';'; outer i += 1 below advances past it
                            val entity = requireNotNull(entityMap[name]) { "Unknown entity: $name" }
                            if (entity.isSimple) {
                                append(entity.simpleValue)
                            } else {
                                val replacement = entity.resolveEmbeddedEntities(entityMap)
                                append(replacement)
                            }
                        }
                    }

                    else -> append(replacementValue[i])
                }
                i += 1
            }
        }
    }


}
