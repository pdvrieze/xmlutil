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

import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI

@XPathInternal
sealed class ArrowFunctionSpecifier {
    abstract fun appendToString(builder: StringBuilder, output: XmlWriter?)

    class QNameFunc(val qname: QName) : ArrowFunctionSpecifier() {
        override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
            when (qname.namespaceURI) {
                "" -> builder.append(qname.localPart)
                else -> when (val p = output?.getPrefix(qname.namespaceURI)) {
                    null -> builder.append("Q{").append(qname.namespaceURI).append("}").append(qname.localPart)
                    else -> builder.append(p).append(':').append(qname.localPart)
                }
            }
        }
    }

    class SeqFunc internal constructor(val elements: List<ExprSingle>): ArrowFunctionSpecifier() {
        init {
            require(elements.isNotEmpty()) {"SeqFunc must have at least one element"}
        }
        internal constructor(p: ParenExpr): this(
            when(val c = p.expr) {
                is SequenceExpr -> c.elements
                is ExprSingle -> listOf(c)
            }
        )

        override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
            builder.append('(')
            val it = elements.iterator()
            it.next().appendToString(builder, output)
            while (it.hasNext()) {
                builder.append(", ")
                it.next().appendToString(builder, output)
            }
            builder.append(')')
        }
    }

    class VarRefFunc internal constructor(val varName: String): ArrowFunctionSpecifier() {
        override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
            builder.append('$').append(varName)
        }
    }
}

