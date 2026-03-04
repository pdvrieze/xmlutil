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

package io.github.pdvrieze.xml.schematypes.impl

import nl.adaptivity.xmlutil.isXmlWhitespace

fun String.rawStringToCollapsedSequence(): Sequence<String> {
    val original = this
    return sequence {
        var newStart = 0
        var i = 0
        val l = original.length
        while (i < l) {
            when(original[i]) {
                '\t', '\n', '\r', ' ' -> {
                    if (i > newStart) yield(original.substring(newStart, i))
                    do {
                        i += 1
                    } while (i < l && isXmlWhitespace(original[i]))
                    newStart = i
                }

                else -> i += 1
            }
        }
    }


}
