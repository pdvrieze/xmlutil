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

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.xml.schematypes.types.AnyType
import nl.adaptivity.xmlutil.NamespaceContext
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.SimpleNamespaceContext

@XPathInternal
class ExprEvalContext(
    namespaceContext: NamespaceContext,
    contextItem: ContextItem?,
    val expr: Expr,
    isXPath1compat: Boolean = false,
    variables: Map<String, Map<String, XdmValue<*>>> = emptyMap(),
    deterministicState: DeterministicState
) : EvalContext(contextItem, namespaceContext, isXPath1compat, variables, deterministicState) {

    @XPathInternal
    fun resolveType(name: QName): AnyType {
        return resolveTypeOrNull(name) ?: throw EvaluationException.Companion(
            ErrorCodes.XPTY0004_TYPE_ERROR,
            "Unknown type $name"
        )
    }

    override fun copy(contextItem: ContextItem?): ExprEvalContext =
        ExprEvalContext(namepaceContext, contextItem, expr, isXPath1Compat, variables, deterministicState)

    fun copy(
        namespaceContext: NamespaceContext = this.namepaceContext,
        contextItem: ContextItem? = this.contextItem,
        expr: Expr = this.expr,
        isXPath1compat: Boolean = this.isXPath1Compat,
        variables: Map<String, Map<String, XdmValue<*>>> = this.variables,
    ): ExprEvalContext = ExprEvalContext(namespaceContext, contextItem, expr, isXPath1compat, variables, deterministicState)

    inline fun <R> withValueContext(value: ContextItem, function: context(ExprEvalContext)  () -> R): R {
        return context(this.copy(contextItem = value), function)
    }

    inline fun <R> withValueContext(value: XdmValue<*>, pos: Int, size: Int, function: context(ExprEvalContext)  () -> R): R {
        val contextItem = ContextItem(value, pos, size)
        return context(copy(contextItem), function)
    }

    override fun newVarScope(
        varName: QName,
        value: XdmValue<*>
    ): ExprEvalContext {
        val newVars = newVarMap(varName, value)
        return copy(variables = newVars)
    }

    companion object {
        val DUMMY =
            ExprEvalContext(SimpleNamespaceContext(), null, ContextItemExpr, deterministicState = DeterministicState())
    }
}
