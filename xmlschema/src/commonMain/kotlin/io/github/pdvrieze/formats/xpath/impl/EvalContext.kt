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

import io.github.pdvrieze.formats.xpath.SpecVersion
import io.github.pdvrieze.formats.xpath.eval.Collations
import io.github.pdvrieze.formats.xpath.eval.data.XdmBuiltinFunction
import io.github.pdvrieze.formats.xpath.eval.data.XdmFunction
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.data.dom.XdmDOMImplementation
import io.github.pdvrieze.formats.xpath.eval.data.dom.XdmDocument
import io.github.pdvrieze.formats.xpath.eval.resolveCollation
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunction
import io.github.pdvrieze.formats.xpath.functions.Fn
import io.github.pdvrieze.xml.schematypes.Collation
import io.github.pdvrieze.xml.schematypes.types.AnyType
import io.github.pdvrieze.xml.schematypes.types.builtinType
import io.github.pdvrieze.xml.schematypes.values.XsdAnyURI
import io.github.pdvrieze.xml.schematypes.values.XsdDateTimeStamp
import io.github.pdvrieze.xml.schematypes.values.XsdLanguage
import io.github.pdvrieze.xml.schematypes.values.formatters.DecimalFormat
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.TimeZone
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.offsetAt
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.internal.QNameMap
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
    val contextItem: ContextItem?,
    val namepaceContext: NamespaceContext,
    val isXPath1Compat: Boolean = false,
    val variables: Map<String, Map<String, XdmValue<*>>> = emptyMap(),
    protected val deterministicState: DeterministicState = DeterministicState(),
    val specVersion: SpecVersion
) {
    constructor(
        baseURI: XsdAnyURI?,
        contextItem: ContextItem? = null,
        namepaceContext: NamespaceContext = SimpleNamespaceContext(),
        isXPath1Compat: Boolean = false,
        specVersion: SpecVersion,
    ) : this(
        contextItem,
        namepaceContext,
        isXPath1Compat,
        deterministicState = DeterministicState(baseURI),
        specVersion = specVersion
    )

    val contextValue get() = contextItem?.value
    val currentTimeStamp: XsdDateTimeStamp get() = deterministicState.currentDateTimeStamp
    val baseUri: XsdAnyURI? get() = deterministicState.baseURI
    val defaultCollation: Collation get() = Collations.CODEPOINT
    val defaultTimeZone: FixedOffsetTimeZone get() = deterministicState.defaultTimeZone

    val defaultLanguage: XsdLanguage get() = deterministicState.defaultLanguage

    val environmentVariables: Map<String, String> get() = deterministicState.environmentVariables

    val collections: Map<XsdAnyURI, XdmValue<*>> get() = deterministicState.collections

    val outputDocument: XdmDocument by lazy {
        XdmDOMImplementation.createDocument(null, null, null)
    }

    fun collation(uri: String): Collation? {
        return resolveCollation(uri)
    }

    fun trace(label: String?, value: String) =
        deterministicState.addTrace(Trace(label, value))

    fun resolveTypeOrNull(name: QName): AnyType? {
        return builtinType(name.localPart, name.namespaceURI)
    }

    val defaultDecimalFormat: DecimalFormat get() = deterministicState.defaultDecimalFormat

    fun resolveDecimalFormat(name: QName): DecimalFormat? {
        return deterministicState.resolveDecimalFormat(name)
    }

    open fun copy(contextValue: XdmValue<*>, contextPos: Int, contextSize: Int): EvalContext {
        return copy(ContextItem(contextValue, contextPos, contextSize))
    }

    open fun copy(contextItem: ContextItem?): EvalContext =
        EvalContext(contextItem, namepaceContext, isXPath1Compat, variables, deterministicState, specVersion)

    fun copyNoExpr(
        contextItem: ContextItem? = this.contextItem,
        namepaceContext: NamespaceContext = this.namepaceContext,
        isXPath1Compat: Boolean = this.isXPath1Compat
    ): EvalContext = EvalContext(
        contextItem,
        namepaceContext,
        isXPath1Compat,
        variables,
        deterministicState,
        specVersion,
    )

    @PublishedApi
    internal fun createExprContext(expr: Expr): ExprEvalContext = ExprEvalContext(
        namepaceContext,
        contextItem,
        expr,
        isXPath1Compat,
        variables,
        deterministicState,
        specVersion
    )

    @OptIn(ExperimentalContracts::class)
    inline fun <R> withExprContext(expr: Expr, block: context(ExprEvalContext) ()-> R): R {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return block(createExprContext(expr))
    }

    open fun newVarScope(varName: QName, value: XdmValue<*>): EvalContext {
        val newVars = newVarMap(varName, value)
        return EvalContext(contextItem, namepaceContext, isXPath1Compat, newVars, deterministicState, specVersion)
    }

    open fun newVarsScope(vars: Iterable<Pair<QName, XdmValue<*>>>): EvalContext {
        val newVars = newVarsMap(vars)
        return EvalContext(contextItem, namepaceContext, isXPath1Compat, newVars, deterministicState, specVersion)
    }

    protected fun newVarsMap(vars: Iterable<Pair<QName, XdmValue<*>>>): MutableMap<String, Map<String, XdmValue<*>>> {
        val newVars = variables.toMutableMap()
        for ((varName, value) in vars) {
            val nsMap = newVars[varName.namespaceURI]?.toMutableMap() ?: mutableMapOf()
            nsMap[varName.localPart] = value
            newVars[varName.namespaceURI] = nsMap
        }
        return newVars
    }

    protected fun newVarMap(varName: QName, value: XdmValue<*>): MutableMap<String, Map<String, XdmValue<*>>> {
        val newVars = mutableMapOf<String, Map<String, XdmValue<*>>>()
        newVars.putAll(variables)
        val newVarMap = variables[varName.namespaceURI]?.toMutableMap() ?: mutableMapOf()
        newVarMap[varName.localPart] = value
        newVars[varName.namespaceURI] = newVarMap
        return newVars
    }

    fun resolveFunction(name: QName, arity: Int): XdmFunction<*>? {
        return when (name.getNamespaceURI()) {
            "", // by default empty functions are just mapped to Fn
            BuiltinFunction.FN_NAMESPACE ->
                Fn.of(name.localPart)?.let { c ->
                    c.functionTypes.firstOrNull { it.argTypes.size == arity || (it.isVarArg && arity >= it.argTypes.size - 1) }
                        ?.let { type ->
                            XdmBuiltinFunction(c, type)
                        }
                }

            else -> null
        }
    }

    fun resolveVar(name: QName): XdmValue<*>? {
        return variables[name.namespaceURI]?.get(name.localPart)
    }

    data class Trace(val label: String?, val value: String)

    @ExperimentalXmlUtilApi
    public class DeterministicState(
        val baseURI: XsdAnyURI? = null,
        val defaultDecimalFormat: DecimalFormat = DecimalFormat(),
        val environmentVariables: Map<String, String> = emptyMap(),
        val collections: Map<XsdAnyURI, XdmValue<*>> = emptyMap(),
        decimalFormats: List<DecimalFormat.Named> = emptyList()
    ) {
        private val _traces: MutableList<Trace> = mutableListOf()

        val traces: List<Trace> get() = _traces

        fun addTrace(trace: Trace) {
            _traces.add(trace)
        }

        private val decimalFormats = QNameMap<DecimalFormat>().also { map ->
            for (df in decimalFormats) {
                map[df.name] = df
            }
        }

        fun resolveDecimalFormat(name: QName): DecimalFormat? {
            return decimalFormats[name]
        }

        private val _timeData by lazy {
            // Use a fixed timezone (rather than one that varies on reference time).
            // XPath assumes it is "fixed"
            val now = Clock.System.now()
            val tz = TimeZone.currentSystemDefault().offsetAt(now).asTimeZone()

            tz to XsdDateTimeStamp(Clock.System.now(), tz)
        }

        val currentDateTimeStamp: XsdDateTimeStamp get() = _timeData.second
        val defaultTimeZone: FixedOffsetTimeZone get() = _timeData.first
        val defaultLanguage: XsdLanguage = XsdLanguage("EN")

        val resultDocument: Document by lazy { XdmDocument(null) }
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

