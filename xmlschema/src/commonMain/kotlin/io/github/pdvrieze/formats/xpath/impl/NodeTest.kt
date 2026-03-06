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

import io.github.pdvrieze.xml.schematypes.values.XsdAnyURI
import io.github.pdvrieze.xml.schematypes.values.XsdNCName
import nl.adaptivity.xmlutil.QName

@XPathInternal
internal sealed class NodeTest {
    sealed class NameTest() : NodeTest()

    sealed class NameOrLiteral {
        context(c: OutputContext)
        abstract fun appendToString(builder: Appendable)

        override fun toString(): String = buildString {
            context(OutputContext.EMPTY) {
                appendToString(this)
            }
        }

        class Literal(val literal: String) : NameOrLiteral() {
            context(c: OutputContext)
            override fun appendToString(builder: Appendable) {
                StringLiteral(literal).appendToString(builder)
            }
        }

        class NCName(val name: String) : NameOrLiteral() {
            context(c: OutputContext)
            override fun appendToString(builder: Appendable) {
                builder.append(name)
            }
        }
    }



    class ProcessingInstructionTest(val literal: NameOrLiteral? = null) : NodeTest() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ProcessingInstructionTest) return false

            if (literal != other.literal) return false

            return true
        }

        override fun hashCode(): Int {
            return literal?.hashCode() ?: 0
        }

        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append("processing-instruction(")
            if (literal != null) {
                literal.appendToString(builder)
            }
            builder.append(")")
        }
    }

    class LocalNameTest(val localName: String) : NameTest() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append("*:").append(localName)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as LocalNameTest

            return localName == other.localName
        }

        override fun hashCode(): Int {
            return localName.hashCode()
        }

    }

    class QNameTest(val qName: QName) : NameTest() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.appendQName(qName)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is QNameTest) return false

            if (qName != other.qName) return false

            return true
        }

        override fun hashCode(): Int {
            return qName.hashCode()
        }
    }

    class NSTest(val namespace: XsdAnyURI, val prefix: XsdNCName? = null) : NameTest() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is NSTest) return false

            if (namespace != other.namespace) return false

            return true
        }

        override fun hashCode(): Int {
            return namespace.hashCode()
        }

        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            val pr = when (c) {
                is OutputContext.WriterCtx -> {
                    c.output.namespaceContext.getPrefixes(namespace.xmlString).let {
                        when {
                            ! it.hasNext() -> null
                            else -> {
                                val f = it.next()
                                when {
                                    prefix == null -> f
                                    it.asSequence().any { it == prefix.xmlString } -> prefix.xmlString
                                    else -> f
                                }
                            }
                        }
                    }
                }
                else -> null
            }

            when (pr) {
                null -> builder.append("Q{").append(namespace.xmlString).append('}')
                else -> builder.append(pr)
            }
            builder.append(":*")
        }

    }

    object AnyNameTest : NameTest() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append("*")
        }
    }

    context(c: OutputContext)
    abstract fun appendToString(builder: Appendable)

    override fun toString(): String = buildString {
        context(OutputContext.EMPTY) {
            appendToString(this)
        }
    }

    companion object {
        val node: NodeTypeTest = NodeTypeTest(NodeType.ANY_KIND)
    }
}

