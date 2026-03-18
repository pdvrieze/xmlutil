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

import io.github.pdvrieze.formats.xpath.XPathExpression
import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.formats.xpath.impl.functions.Fn
import io.github.pdvrieze.xml.schematypes.types.TokenType
import io.github.pdvrieze.xml.schematypes.values.XsdToken

@OptIn(XPathInternal::class)
internal class XPathExpressionImpl internal constructor(
    override val xmlString: String,
    override val expr: Expr,
    override val version: XPathVersion,
) : XsdToken, XPathExpression {

    override val schemaType: TokenType<XsdToken> get() = TokenType.Instance

    init {
        val unsupportedExprs = mutableListOf<Any>()
        expr.collectUnsupportedExprs(version, false, unsupportedExprs)
        if (unsupportedExprs.isNotEmpty()) {
            throw IllegalArgumentException(
                "Unsupported expressions in XPath $version expression: ${unsupportedExprs.joinToString(", ")}"
            )
        }
    }

    companion object {

        // TODO: Make including this configurable

        // Opt in as it changes the semantics and is thus needed for XPath 1 too
        @OptIn(XPathInternal::class, NeedsXPath2::class)
        internal val STEP_DOC_ROOT = FilterExpr(
            TreatAsExpr(
                StaticFunctionCall(Fn.root.name, LocationPath(AxisStep(Axis.SELF, NodeTest.node))),
                SequenceType.ItemTypeSequence(ItemTypeTest.documentNode, SequenceType.OccurrenceType.ANY)
            ),
            emptyList()
        )

    }


}

