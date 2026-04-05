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

package io.github.pdvrieze.formats.xpath.impl.token

import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.formats.xpath.eval.ErrorCodes.XPTY0020_CONTEXT_ITEM_NOT_NODE
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmNode
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmSingleValue
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath1
import io.github.pdvrieze.formats.xpath.impl.NodeTest
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import nl.adaptivity.xmlutil.dom2.*

enum class Axis(val literal: String, val minVersion: XPathVersion = XPathVersion.XPath1_0) : Token {
    @NeedsXPath1
    CHILD("child") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<IndexedValue<XdmValue<*>>> {
            var i = 0
            return context.node.getChildNodes().map { IndexedValue(++i, XdmNode(it)) }
        }
    },

    @NeedsXPath1
    DESCENDANT("descendant") {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun elementSequence(context: XdmNode): List<IndexedValue<XdmValue<*>>> {
            var i = 0
            return context.descendantsSequence().mapTo(ArrayList()) { IndexedValue(++i, it) }
        }
    },

    @NeedsXPath1
    PARENT("parent") {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun elementSequence(context: XdmNode): List<IndexedValue<XdmValue<*>>> {
            return listOfNotNull(context.node.parentNode?.let { IndexedValue(1, XdmNode(it)) })
        }
    },

    @NeedsXPath1
    ANCESTOR("ancestor") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<IndexedValue<XdmValue<*>>> {
            var node = context.node.parentNode ?: return emptyList()
            val result = ArrayDeque<IndexedValue<XdmNode>>()
            var i = 0
            do {
                result.addFirst(IndexedValue(++i, XdmNode(node)))
                node = node.parentNode ?: break

            } while (node is Element)
            return result
        }
    },

    @NeedsXPath1
    FOLLOWING_SIBLING("following-sibling") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<IndexedValue<XdmValue<*>>> {
            var i = 0
            return buildList {
                var node = context.node.nextSibling
                while (node != null) {
                    add(IndexedValue(++i, XdmNode(node)))
                    node = node.nextSibling
                }
            }
        }
    },

    @NeedsXPath1
    PRECEDING_SIBLING("preceding-sibling") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<IndexedValue<XdmValue<*>>> {
            var i = 0
            val result = ArrayDeque<IndexedValue<XdmNode>>()
            var node = context.node.previousSibling
            while (node != null) {
                result.addFirst(IndexedValue(++i, XdmNode(node)))
                node = node.previousSibling
            }
            return result

        }
    },

    @NeedsXPath1
    FOLLOWING("following") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<IndexedValue<XdmValue<*>>> {
            var i = 0
            return buildList {
                //addAll(context.descendantsSequence())
                var p: Node? = context.node
                while (p is Element) {
                    var s = p.nextSibling
                    while (s != null) {
                        val xdmNode = XdmNode(s)
                        add(IndexedValue(++i, xdmNode))
                        xdmNode.descendantsSequence().mapTo(this) {
                            IndexedValue(++i, it)
                        }

                        s = s.nextSibling
                    }
                    p = p.parentNode
                }
            }
        }
    },

    @NeedsXPath1
    PRECEDING("preceding") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<IndexedValue<XdmValue<*>>> {
            val result = ArrayDeque<IndexedValue<XdmNode>>()
            var i = 0
            var p: Node? = context.node
            while (p is Element) {
                var s = p.previousSibling
                while (s != null) {
                    val xdmNode = XdmNode(s)
                    result.addFirst(IndexedValue(++i, xdmNode))
                    for (n in xdmNode.descendantsSequence()) {
                        result.addFirst(IndexedValue(++i, n))
                    }
                    s = s.previousSibling
                }
                p = p.parentNode // the loop will get the previous sibling anyway
            }

            return result
        }
    },

    @NeedsXPath1
    ATTRIBUTE("attribute") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<IndexedValue<XdmValue<*>>> {
            return when (val node = context.node) {
                is Element -> {
                    var i = 0
                    node.attributes.map { IndexedValue(++i, XdmNode(it)) }
                }

                else -> emptyList()
            }
        }
    },

    @NeedsXPath1
    NAMESPACE("namespace") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<IndexedValue<XdmValue<*>>> {
            return emptyList()
            /*
                        val node = context.node as? Element ?: return emptyList()
                        val l= node.attributes.asSequence()
                            .filter { it.namespaceURI == XMLConstants.XMLNS_ATTRIBUTE_NS_URI }
                            .map {
                                val prefix = it.name.substringAfterLast(':')
                                val ns = it.value
                                ctx.outputDocument.create
                            }

                        return super.elementSequence(context)
            */
        }
    },

    @NeedsXPath1
    SELF("self") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<IndexedValue<XdmValue<*>>> {
            return listOf(IndexedValue(1, context))
        }
    },

    @NeedsXPath1
    DESCENDANT_OR_SELF("descendant-or-self") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<IndexedValue<XdmValue<*>>> {
            return buildList {
                add(IndexedValue(1, context))
                var i = 1
                context.descendantsSequence().mapTo(this) { IndexedValue(++i, it) }
            }
        }
    },

    @NeedsXPath1
    ANCESTOR_OR_SELF("ancestor-or-self") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<IndexedValue<XdmValue<*>>> {
            val result = ArrayDeque<IndexedValue<XdmNode>>()
            var i = 0

            result.add(IndexedValue(++i, context))
            var p: Node? = context.node.parentNode
            while (p is Element) {
                result.addFirst(IndexedValue(++i, XdmNode(p)))
                p = p.parentNode
            }
            return result
        }
    },
    ;

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun eval(context: XdmValue<*>, test: NodeTest): XdmValue<*> {
        return when (context) {
            is XdmSequence<*> -> {
                XdmSequence.build<XdmSingleValue<*>> {
                    val seen = HashSet<XdmValue<*>>()
                    for (e in context.elements) {
                        when (val value = eval(e, test)) {
                            is XdmSequence<*> -> {
                                for (v in value.elements) {
                                    if (seen.add(v)) add(v)
                                }
                            }

                            is XdmSingleValue<*> -> if (seen.add(value)) add(value)
                        }
                    }
                }
            }

            is XdmNode -> evalNode(context, test)

            else -> throw EvaluationException(
                XPTY0020_CONTEXT_ITEM_NOT_NODE,
                "Context items for axes ($literal) must be nodes (found: ${context.staticType})"
            )
        }
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    abstract fun elementSequence(context: XdmNode): List<IndexedValue<XdmValue<*>>>

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun evalNode(context: XdmNode, test: NodeTest): XdmValue<*> {
        val axisElementSequence = elementSequence(context)
        return XdmSequence.build<XdmSingleValue<*>> {
            for ((idx, elem) in axisElementSequence) {
                if (test.eval(elem, idx, axisElementSequence.size)) {
                    add(elem)
                }
            }
        }
    }

    override val isDelimiting: Boolean
        get() = false

    companion object {
        private val lookup = entries.associateBy { it.literal }

        fun from(value: String): Axis {
            return requireNotNull(lookup[value]) { "$value is not a valid path axis" }
        }
    }


}
