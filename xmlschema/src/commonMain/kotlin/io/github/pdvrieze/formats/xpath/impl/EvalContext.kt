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

import io.github.pdvrieze.formats.xpath.eval.Collation
import io.github.pdvrieze.formats.xpath.eval.Collations
import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.xml.schematypes.types.AnyType
import io.github.pdvrieze.xml.schematypes.types.builtinType
import io.github.pdvrieze.xml.schematypes.values.XsdAnyURI
import io.github.pdvrieze.xml.schematypes.values.XsdDateTimeStamp
import kotlinx.datetime.TimeZone
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.dom2.Document
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Clock

/**
 * @property deterministicState State that does not change during the evaluation of the expression
 */
@XPathInternal
open class EvalContext(
    val namepaceContext: NamespaceContext,
    val contextItem: ContextItem?,
    val isXPath1Compat: Boolean = false,
    val variables: Map<String, Map<String, XdmValue<*>>> = emptyMap(),
    protected val deterministicState: DeterministicState = DeterministicState()
) {
    constructor(
        baseURI: XsdAnyURI?,
        contextItem: ContextItem? = null,
        namepaceContext: NamespaceContext = SimpleNamespaceContext(),
        isXPath1Compat: Boolean = false,
    ) : this(namepaceContext, contextItem, isXPath1Compat, emptyMap(), DeterministicState(baseURI))

    val contextValue get() = contextItem?.value
    val currentTimeStamp: XsdDateTimeStamp get() = deterministicState.currentDateTimeStamp
    val baseUri: XsdAnyURI? get() = deterministicState.baseURI
    val defaultCollation: Collation get() = Collations.CODEPOINT

    fun resolveTypeOrNull(name: QName): AnyType? {
        return builtinType(name.localPart, name.namespaceURI)
    }

    val outputDocument: Document by lazy {
        xmlStreaming.genericDomImplementation.createDocument(null, null, null)
    }

    open fun copy(contextValue: XdmValue<*>, contextPos: Int, contextSize: Int): EvalContext {
        return copy(ContextItem(contextValue, contextPos, contextSize))
    }

    open fun copy(contextItem: ContextItem?): EvalContext =
        EvalContext(namepaceContext, contextItem, isXPath1Compat, variables, deterministicState)

    fun copyNoExpr(
        contextItem: ContextItem? = this.contextItem,
        namepaceContext: NamespaceContext = this.namepaceContext,
        isXPath1Compat: Boolean = this.isXPath1Compat
    ): EvalContext = EvalContext(namepaceContext, contextItem, isXPath1Compat, variables, deterministicState)

    @PublishedApi
    internal fun createExprContext(expr: Expr): ExprEvalContext = ExprEvalContext(namepaceContext, contextItem, expr, isXPath1Compat, variables, deterministicState)

    @OptIn(ExperimentalContracts::class)
    inline fun <R> withExprContext(expr: Expr, block: context(ExprEvalContext) ()-> R): R {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return block(createExprContext(expr))
    }

    open fun newVarScope(varName: QName, value: XdmValue<*>): EvalContext {
        val newVars = newVarMap(varName, value)
        return EvalContext(namepaceContext, contextItem, isXPath1Compat, newVars, deterministicState)
    }

    protected fun newVarMap(varName: QName, value: XdmValue<*>): MutableMap<String, Map<String, XdmValue<*>>> {
        val newVars = mutableMapOf<String, Map<String, XdmValue<*>>>()
        newVars.putAll(variables)
        val newVarMap = variables[varName.namespaceURI]?.toMutableMap() ?: mutableMapOf()
        newVarMap[varName.localPart] = value
        newVars[varName.namespaceURI] = newVarMap
        return newVars
    }

    class DeterministicState(
        val baseURI: XsdAnyURI? = null,
    ) {
        val currentDateTimeStamp by lazy {
            XsdDateTimeStamp(Clock.System.now(), TimeZone.currentSystemDefault())
        }
    }

}

@OptIn(XPathInternal::class)
inline fun <C: EvalContext, R> C.withValueContext(value: ContextItem, function: context(C)  () -> R): R {

    @Suppress("UNCHECKED_CAST")
    return context(copy(contextItem = value) as C, function)
}

@OptIn(XPathInternal::class)
inline fun <C: EvalContext, R> C.withValueContext(value: XdmValue<*>, pos: Int, size: Int, function: context(C)  () -> R): R {

    @Suppress("UNCHECKED_CAST")
    return context(copy(contextItem = ContextItem(value, pos, size)) as C, function)
}

class ContextItem(val value: XdmValue<*>, val position: Int, val last: Int)

@XPathInternal
class ExprEvalContext(
    namespaceContext: NamespaceContext,
    contextItem: ContextItem?,
    val expr: Expr,
    isXPath1compat: Boolean = false,
    variables: Map<String, Map<String, XdmValue<*>>> = emptyMap(),
    deterministicState: DeterministicState
) : EvalContext(namespaceContext, contextItem, isXPath1compat, variables, deterministicState) {

    @XPathInternal
    fun resolveType(name: QName): AnyType {
        return resolveTypeOrNull(name) ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Unknown type $name")
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

