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
import io.github.pdvrieze.formats.xpath.data.EvaluationException
import io.github.pdvrieze.formats.xpath.data.EvaluationException.ErrorCodes.XPTY0020_CONTEXT_ITEM_NOT_NODE
import io.github.pdvrieze.formats.xpath.data.XdmNode
import io.github.pdvrieze.formats.xpath.data.XdmSequence
import io.github.pdvrieze.formats.xpath.data.XdmValue
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath1
import io.github.pdvrieze.formats.xpath.impl.NodeTest
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import nl.adaptivity.xmlutil.dom2.*

enum class Axis(val literal: String, val minVersion: XPathVersion = XPathVersion.XPath1_0): Token {
    @NeedsXPath1
    CHILD("child") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<XdmValue> {
            return context.node.getChildNodes().map { XdmNode(it)}
        }
    },
    @NeedsXPath1
    DESCENDANT("descendant") {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun elementSequence(context: XdmNode): List<XdmValue> {
            return context.descendantsSequence().toList()
        }
    },
    @NeedsXPath1
    PARENT("parent") {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun elementSequence(context: XdmNode): List<XdmValue> {
            return listOfNotNull(context.node.parentNode?.let { XdmNode(it) })
        }
    },
    @NeedsXPath1
    ANCESTOR("ancestor") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<XdmValue> {
            var node= context.node.parentNode ?: return emptyList()
            return buildList {
                do {
                    add(XdmNode(node))
                    node = node.parentNode ?: break
                } while (node is Element)
            }
        }
    },
    @NeedsXPath1
    FOLLOWING_SIBLING("following-sibling") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<XdmValue> {
            return buildList {
                var node = context.node.nextSibling
                while (node != null) {
                    add(XdmNode(node))
                    node = node.nextSibling
                }
            }
        }
    },
    @NeedsXPath1
    PRECEDING_SIBLING("preceding-sibling") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<XdmValue> {
            return buildList {
                var node = context.node.previousSibling
                while (node != null) {
                    add(XdmNode(node))
                    node = node.previousSibling
                }
            }

        }
    },
    @NeedsXPath1
    FOLLOWING("following") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<XdmValue> {
            return buildList {
                //addAll(context.descendantsSequence())
                var p: Node? = context.node
                while (p is Element) {
                    var s= p.nextSibling
                    while (s != null) {
                        val xdmNode = XdmNode(s)
                        add(xdmNode)
                        addAll(xdmNode.descendantsSequence())
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
        override fun elementSequence(context: XdmNode): List<XdmValue> {
            return buildList {
                var p: Node? = context.node
                while (p is Element) {
                    var s = p.previousSibling
                    while (s != null) {
                        val xdmNode = XdmNode(s)
                        add(xdmNode)
                        addAll(xdmNode.descendantsSequence().toList().reversed())
                        s = s.previousSibling
                    }
                    p = p.parentNode // the loop will get the previous sibling anyway
                }
            }

        }
    },
    @NeedsXPath1
    ATTRIBUTE("attribute"){
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<XdmValue> {
            return when (val node= context.node) {
                is Element -> node.attributes.map { XdmNode(it) }

                else -> emptyList()
            }
        }
    },
    @NeedsXPath1
    NAMESPACE("namespace") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<XdmValue> {
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
        override fun elementSequence(context: XdmNode): List<XdmValue> {
            return listOf(context)
        }
    },

    @NeedsXPath1
    DESCENDANT_OR_SELF("descendant-or-self") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<XdmValue> {
            return buildList {
                add(context)
                addAll(context.descendantsSequence())
            }
        }
    },

    @NeedsXPath1
    ANCESTOR_OR_SELF("ancestor-or-self") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun elementSequence(context: XdmNode): List<XdmValue> {
            return buildList {
                add(context)
                var p: Node? = context.node.parentNode
                while (p is Element) {
                    add(XdmNode(p))
                    p = p.parentNode
                }
            }
        }
    },
    ;

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun eval(context: XdmValue, test: NodeTest): XdmValue {
        return when (context) {
            is XdmSequence<*> -> {
                val newNodes: List<XdmValue> = context.flatMapTo(LinkedHashSet()) {
                    when (val v = eval(it, test)) {
                        is XdmSequence<*> -> v
                        else -> listOf(v)
                    }
                }.toList()
                XdmSequence(newNodes)
            }

            is XdmNode -> evalNode(context, test)

            else -> throw EvaluationException(XPTY0020_CONTEXT_ITEM_NOT_NODE)
        }
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    open fun elementSequence(context: XdmNode): List<XdmValue> {
        TODO("Axis sequences not implemented yet")
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun evalNode(context: XdmNode, test: NodeTest): XdmValue {
        val axisElementSequence = elementSequence(context)
        val result = axisElementSequence.filterIndexed { index, value ->
            test.eval(value, index, axisElementSequence.size)
        }
        return when (axisElementSequence.size) {
            1 if result.size == 1 -> result.single()
            else -> XdmSequence(result)
        }
    }

    final override val isDelimiting: Boolean
        get() = false

    companion object {
        private val lookup = entries.associateBy { it.literal }

        fun from(value: String): Axis {
            return requireNotNull(lookup[value]) { "$value is not a valid path axis" }
        }
    }


}
