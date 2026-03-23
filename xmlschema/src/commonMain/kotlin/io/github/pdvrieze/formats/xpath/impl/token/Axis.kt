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
import io.github.pdvrieze.formats.xpath.data.XdmNode
import io.github.pdvrieze.formats.xpath.data.XdmSequence
import io.github.pdvrieze.formats.xpath.data.XdmValue
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath1
import io.github.pdvrieze.formats.xpath.impl.NodeTest
import io.github.pdvrieze.formats.xpath.impl.XPathInternal

enum class Axis(val literal: String, val minVersion: XPathVersion = XPathVersion.XPath1_0): Token {
    @NeedsXPath1
    CHILD("child") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalNode(
            context: XdmNode,
            test: NodeTest
        ): XdmValue {
            val xmlNode= context.node
            val children= xmlNode.getChildNodes().asSequence()
            val result= children
                .map { XdmNode(it) }
                .filter { test.eval(it) }
                .toList()

            return XdmSequence(result)
        }
    },
    @NeedsXPath1
    DESCENDANT("descendant"),
    @NeedsXPath1
    PARENT("parent"),
    @NeedsXPath1
    ANCESTOR("ancestor"),
    @NeedsXPath1
    FOLLOWING_SIBLING("following-sibling"),
    @NeedsXPath1
    PRECEDING_SIBLING("preceding-sibling"),
    @NeedsXPath1
    FOLLOWING("following"),
    @NeedsXPath1
    PRECEDING("preceding"),
    @NeedsXPath1
    ATTRIBUTE("attribute"),
    @NeedsXPath1
    NAMESPACE("namespace"),
    @NeedsXPath1
    SELF("self"),
    @NeedsXPath1
    DESCENDANT_OR_SELF("descendant-or-self") {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalNode(
            context: XdmNode,
            test: NodeTest
        ): XdmValue {
            val result= mutableListOf<XdmValue>()
            for (desc in sequenceOf(context) + context.descendantsSequence()) {
                if (test.eval(desc)) result.add(desc)
            }
            return XdmSequence(result)
        }
    },
    @NeedsXPath1
    ANCESTOR_OR_SELF("ancestor-or-self"),
    ;

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun eval(context: XdmValue, test: NodeTest): XdmValue {
        return when (context) {
            is XdmSequence<*> -> context.flatMap { i -> eval(i, test) }
            is XdmNode -> evalNode(context, test)
            else -> TODO("Evaluation of axis $literal is not yet implemented")
        }
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    open fun evalNode(context: XdmNode, test: NodeTest): XdmValue {
        return if (test.eval(context)) context else XdmSequence.EMPTY
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
