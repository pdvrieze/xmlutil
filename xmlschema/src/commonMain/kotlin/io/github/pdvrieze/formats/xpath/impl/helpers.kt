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

package io.github.pdvrieze.formats.xpath.impl

import nl.adaptivity.xmlutil.*

internal inline fun <A: Appendable, T> A.joinHelper(collection: Iterable<T>, separator: String = ", ", action: A.(T)-> Unit) {
    val it = collection.iterator()
    if (it.hasNext()) {
        action(it.next())
        while (it.hasNext()) {
            append(separator)
            action(it.next())
        }
    }
}

@XPathInternal
context(c: OutputContext)
internal fun <A: Appendable> A.appendExprs(collection: Iterable<Expr>, separator: String = ", ") {
    val it = collection.iterator()
    if (it.hasNext()) {
        it.next().appendToString(this)
        while (it.hasNext()) {
            append(separator)
            it.next().appendToString(this)
        }
    }
}

@IgnorableReturnValue
context(c: OutputContext) @XPathInternal
internal fun <A : Appendable> A.appendQName(name: QName): A {
    if (c !is OutputContext.WriterCtx) {
        if (name.namespaceURI.isEmpty()) {
            append(name.localPart)
        } else {
            append("Q{").append(name.namespaceURI).append("}").append(name.localPart)
        }
    } else {
        val prefixes = c.output.namespaceContext.getPrefixes(name.namespaceURI)
        val it = prefixes.iterator()
        if (!it.hasNext()) {
            append("Q{").append(name.namespaceURI).append("}").append(name.localPart)
        } else {

            val firstPrefix = it.next()
            val prefix: String = when {
                it.asSequence().any { it == name.prefix } -> name.prefix
                else -> firstPrefix
            }

            if (!prefix.isEmpty()) append(prefix).append(':')
            append(name.localPart)
        }
    }
    return this
}

@XPathInternal
interface OutputContext {
    class WriterCtx(val output: XmlWriter): OutputContext
    object EMPTY: OutputContext

    companion object {
        operator fun invoke(output: XmlWriter): OutputContext = WriterCtx(output)
        operator fun invoke(n: Nothing?): OutputContext = EMPTY
    }
}

