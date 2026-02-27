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

@XPathInternal
sealed class Expr {
    abstract fun appendToString(builder: StringBuilder, output: XmlWriter?)

    final override fun toString(): String = buildString {
        appendToString(this, null)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        return true
    }

    override fun hashCode(): Int {
        return this::class.hashCode()
    }


}

@XPathInternal
internal fun StringBuilder.appendQName(
    name: QName,
    output: XmlWriter?
) {
    if (output == null) {
        if (name.namespaceURI.isEmpty()) {
            append(name.localPart)
        } else {
            append("Q{").append(name.namespaceURI).append("}").append(name.localPart)
        }
        return
    } else {
        val prefixes = output.namespaceContext.getPrefixes(name.namespaceURI)
        val it = prefixes.iterator()
        if (!it.hasNext()) {
            append("Q{").append(name.namespaceURI).append("}").append(name.localPart)
            return
        }

        val firstPrefix = it.next()
        val prefix: String = when {
            it.asSequence().any { it == name.prefix } -> name.prefix
            else -> firstPrefix
        }

        if (! prefix.isEmpty()) append(prefix).append(':')
        append(name.localPart)
    }
}


@OptIn(XPathInternal::class)
sealed class ExprSingle(): Expr() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false
        return true
    }

}
