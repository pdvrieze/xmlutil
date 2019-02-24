/*
 * Copyright (c) 2017.
 *
 * This file is part of XmlUtil.
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

package nl.adaptivity.js.util

import org.w3c.dom.*
import kotlin.dom.isElement
import kotlin.dom.isText

fun Node.asElement(): Element? = if (isElement) this as Element else null
fun Node.asText(): Text? = if (isText) this as Text else null

fun Node.removeElementChildren() {
    val top = this
    var cur = top.firstChild
    while (cur != null) {
        val n = cur.nextSibling
        if (cur.isElement) {
            top.removeChild(cur)
        }
        cur = n
    }
}
