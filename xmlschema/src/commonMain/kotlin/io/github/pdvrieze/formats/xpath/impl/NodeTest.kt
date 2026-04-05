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

import io.github.pdvrieze.formats.xpath.eval.data.XdmNode
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.xml.schematypes.values.XsdAnyURI
import io.github.pdvrieze.xml.schematypes.values.XsdNCName
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.dom2.*
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI

@XPathInternal
@NeedsXPath1
sealed class NodeTest {
    sealed class NameTest : NodeTest()

    sealed class NameOrLiteral {
        context(c: OutputContext)
        abstract fun appendToString(builder: Appendable)

        override fun toString(): String = buildString {
            context(OutputContext.EMPTY) {
                appendToString(this)
            }
        }

        class LiteralTest(val literal: String) : NameOrLiteral() {
            context(c: OutputContext)
            override fun appendToString(builder: Appendable) {
                StringLiteral(literal).appendToString(builder)
            }
        }

        class NCNameTest(val name: String) : NameOrLiteral() {
            context(c: OutputContext)
            override fun appendToString(builder: Appendable) {
                builder.append(name)
            }
        }
    }



    class ProcessingInstructionTest(val literal: NameOrLiteral? = null) : NodeTest() {
        context(ctx: ExprEvalContext)
        override fun eval(it: XdmValue<*>, index: Int, count: Int): Boolean {
            val pi = ((it as? XdmNode)?.node as? ProcessingInstruction) ?: return false
            return when (literal) {
                null -> true
                is NameOrLiteral.LiteralTest -> literal.literal == pi.data
                is NameOrLiteral.NCNameTest -> literal.name == pi.target
            }
        }

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
        context(ctx: ExprEvalContext)
        override fun eval(
            it: XdmValue<*>,
            index: Int,
            count: Int
        ): Boolean {
            return it is XdmNode && (it.node as? Element)?.localName == localName
        }

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
        context(ctx: ExprEvalContext)
        override fun eval(it: XdmValue<*>, index: Int, count: Int): Boolean {
            if (it !is XdmNode) return false
            return when (val n = it.node) {
                is Attr -> n.localName== qName.localPart && (n.namespaceURI ?: "") == qName.namespaceURI
                is Element -> n.localName== qName.localPart && (n.namespaceURI ?: "") == qName.namespaceURI
                else -> false
            }
        }

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
        context(ctx: ExprEvalContext)
        override fun eval(
            it: XdmValue<*>,
            index: Int,
            count: Int
        ): Boolean {
            return it is XdmNode && (it.node as? Element).let {
                it?.namespaceURI == namespace.xmlString && it.prefix == prefix?.xmlString
            }
        }

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

        context(ctx: ExprEvalContext)
        override fun eval(it: XdmValue<*>, index: Int, count: Int): Boolean {
            return when (it) {
                is XdmNode -> when (it.node) {
                    is Attr,
                    is Element -> true

                    else -> false
                }
                else -> false
            }
        }
    }

    context(c: OutputContext)
    abstract fun appendToString(builder: Appendable)

    override fun toString(): String = buildString {
        context(OutputContext.EMPTY) {
            appendToString(this)
        }
    }

    context(ctx: ExprEvalContext)
    abstract fun eval(it: XdmValue<*>, index: Int, count: Int): Boolean/* {
        TODO("not implemented for ${this::class.simpleName}")
    }*/

    companion object {
        val node: NodeKindTest = NodeKindTest.AnyNode
    }
}

