/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSIdentityConstraint
import io.github.pdvrieze.formats.xpath.XPathExpression
import io.github.pdvrieze.formats.xpath.impl.*
import io.github.pdvrieze.formats.xpath.impl.BinaryExpr
import io.github.pdvrieze.formats.xpath.impl.LocationPath
import io.github.pdvrieze.formats.xpath.impl.NodeTest
import io.github.pdvrieze.formats.xpath.impl.Step

@OptIn(XPathInternal::class)
sealed class ResolvedDirectReferenceable(
    rawPart: XSIdentityConstraint,
    schema: ResolvedSchemaLike,
    owner: ResolvedElement
) : ResolvedNamedIdentityConstraint(rawPart, schema, owner), ResolvedReferenceableConstraint {

    init {
        require(rawPart.fields.isNotEmpty()) { "identity constraint must have at least one field: $rawPart" }
        for (field in rawPart.fields) {
            require(isXsdSubset(field.xpath.expr, true)) { "${field.xpath.xmlString} is not in the field subset" }
        }
        val selector = requireNotNull(rawPart.selector)
        require(isXsdSubset(selector.xpath, false)) { "${selector.xpath.xmlString} is not in the selector subset"}
    }

    private fun isXsdSubset(xPathExpression: XPathExpression, isTrailingAttrAllowed: Boolean): Boolean {
        return isXsdSubset(xPathExpression.expr, isTrailingAttrAllowed)
    }

    private fun isXsdSubset(expr: Expr, isTrailingAttrAllowed: Boolean): Boolean {
        return when (expr) {
            is BinaryExpr -> expr.operator == Operator.UNION &&
                    isXsdSubset(expr.left, isTrailingAttrAllowed) &&
                    isXsdSubset(expr.right, isTrailingAttrAllowed)

            is LocationPath -> {
                if (expr.rooted || expr.steps.size==0) return false
                val firstStep = expr.steps.first()
                val stepIndices: IntRange = if (firstStep.axis == Axis.SELF && expr.steps.size>1 &&
                    expr.steps[1].let { it.axis== Axis.DESCENDANT_OR_SELF && it.test== NodeTest.NodeTypeTest(NodeType.NODE) }) {
                    2 until expr.steps.size
                } else {
                    expr.steps.indices
                }
                return stepIndices.all {
                    isXsdSubset(expr.steps[it], isTrailingAttrAllowed && it + 1 == expr.steps.size)
                }
            }
            else -> false
        }
    }

    private fun isXsdSubset(step: Step, canBeAttr: Boolean = false): Boolean = step.predicates.size == 0 && when(step.axis) {
        Axis.SELF -> step.test == NodeTest.NodeTypeTest(NodeType.NODE)
        Axis.ATTRIBUTE -> canBeAttr && step.test is NodeTest.NameTest
        Axis.CHILD -> step.test is NodeTest.NameTest
        else -> false
    }

}
