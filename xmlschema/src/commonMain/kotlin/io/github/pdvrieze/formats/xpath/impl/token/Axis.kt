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
import io.github.pdvrieze.formats.xpath.eval.data.dom.XdmElement
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath1
import io.github.pdvrieze.formats.xpath.impl.NodeTest
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.dom2.Node

enum class Axis(val literal: String, val minVersion: XPathVersion = XPathVersion.XPath1_0) : Token {
    @NeedsXPath1
    CHILD("child") {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun elementSequence(context: XdmNode<*>): List<IndexedValue<XdmValue<*>>> {
            var i = 0
            return context.getChildNodes().map { IndexedValue(++i, it as XdmNode<*>) }
        }
    },

    @NeedsXPath1
    DESCENDANT("descendant") {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun elementSequence(context: XdmNode<*>): List<IndexedValue<XdmValue<*>>> {
            var i = 0
            return context.descendantsSequence().mapTo(ArrayList()) { IndexedValue(++i, it) }
        }
    },

    @NeedsXPath1
    PARENT("parent") {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun elementSequence(context: XdmNode<*>): List<IndexedValue<XdmValue<*>>> {
            return listOfNotNull(context.getParentNode()?.let { IndexedValue(1, it) })
        }
    },

    @NeedsXPath1
    ANCESTOR("ancestor") {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun elementSequence(context: XdmNode<*>): List<IndexedValue<XdmValue<*>>> {
            var node = context.getParentNode() ?: return emptyList()
            val result = ArrayDeque<IndexedValue<XdmNode<*>>>()
            var i = 0
            do {
                result.addFirst(IndexedValue(++i, (node)))
                node = node.getParentNode() ?: break

            } while (node is Element)
            return result
        }
    },

    @NeedsXPath1
    FOLLOWING_SIBLING("following-sibling") {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun elementSequence(context: XdmNode<*>): List<IndexedValue<XdmValue<*>>> {
            var i = 0
            return buildList {
                var node = context.getNextSibling()
                while (node != null) {
                    add(IndexedValue(++i, node))
                    node = node.getNextSibling()
                }
            }
        }
    },

    @NeedsXPath1
    PRECEDING_SIBLING("preceding-sibling") {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun elementSequence(context: XdmNode<*>): List<IndexedValue<XdmValue<*>>> {
            var i = 0
            val result = ArrayDeque<IndexedValue<XdmNode<*>>>()
            var node = context.getPreviousSibling()
            while (node != null) {
                result.addFirst(IndexedValue(++i, (node)))
                node = node.getPreviousSibling()
            }
            return result

        }
    },

    @NeedsXPath1
    FOLLOWING("following") {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun elementSequence(context: XdmNode<*>): List<IndexedValue<XdmValue<*>>> {
            var i = 0
            return buildList {
                //addAll(context.descendantsSequence())
                var p: Node? = context
                while (p is XdmElement) {
                    var s = p.getNextSibling()
                    while (s != null) {
                        val xdmNode: XdmNode<*> = s
                        add(IndexedValue(++i, xdmNode))
                        xdmNode.descendantsSequence().mapTo(this) {
                            IndexedValue(++i, it)
                        }

                        s = s.getNextSibling()
                    }
                    p = p.getParentNode()
                }
            }
        }
    },

    @NeedsXPath1
    PRECEDING("preceding") {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun elementSequence(context: XdmNode<*>): List<IndexedValue<XdmValue<*>>> {
            val result = ArrayDeque<IndexedValue<XdmNode<*>>>()
            var i = 0
            var p: XdmNode<*>? = context
            while (p is Element) {
                var s = p.getPreviousSibling()
                while (s != null) {
                    val xdmNode = s
                    result.addFirst(IndexedValue(++i, xdmNode))
                    for (n in xdmNode.descendantsSequence()) {
                        result.addFirst(IndexedValue(++i, n))
                    }
                    s = s.getPreviousSibling()
                }
                p = p.getParentNode() // the loop will get the previous sibling anyway
            }

            return result
        }
    },

    @NeedsXPath1
    ATTRIBUTE("attribute") {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun elementSequence(context: XdmNode<*>): List<IndexedValue<XdmValue<*>>> {
            return when (val node = context) {
                is XdmElement -> {
                    var i = 0
                    node.getAttributes().map { IndexedValue(++i, it) }
                }

                else -> emptyList()
            }
        }
    },

    @NeedsXPath1
    NAMESPACE("namespace") {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun elementSequence(context: XdmNode<*>): List<IndexedValue<XdmValue<*>>> {
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
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun elementSequence(context: XdmNode<*>): List<IndexedValue<XdmValue<*>>> {
            return listOf(IndexedValue(1, context))
        }
    },

    @NeedsXPath1
    DESCENDANT_OR_SELF("descendant-or-self") {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun elementSequence(context: XdmNode<*>): List<IndexedValue<XdmValue<*>>> {
            return buildList {
                add(IndexedValue(1, context))
                var i = 1
                context.descendantsSequence().mapTo(this) { IndexedValue(++i, it) }
            }
        }
    },

    @NeedsXPath1
    ANCESTOR_OR_SELF("ancestor-or-self") {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun elementSequence(context: XdmNode<*>): List<IndexedValue<XdmValue<*>>> {
            val result = ArrayDeque<IndexedValue<XdmNode<*>>>()
            var i = 0

            result.add(IndexedValue(++i, context))
            var p: Node? = context.getParentNode()
            while (p is XdmElement) {
                result.addFirst(IndexedValue(++i, (p)))
                p = p.getParentNode()
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
                XdmSequence.buildSingle {
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

            is XdmNode<*> -> evalNode(context, test)

            else -> throw EvaluationException(
                XPTY0020_CONTEXT_ITEM_NOT_NODE,
                "Context items for axes ($literal) must be nodes (found: ${context.staticType})"
            )
        }
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    abstract fun elementSequence(context: XdmNode<*>): List<IndexedValue<XdmValue<*>>>

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun evalNode(context: XdmNode<*>, test: NodeTest): XdmValue<*> {
        val axisElementSequence = elementSequence(context)
        return XdmSequence.buildSingle {
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
