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

@file:OptIn(NeedsXPath2::class)

package io.github.pdvrieze.formats.xpath.functions

import io.github.pdvrieze.formats.xpath.data.*
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath2
import io.github.pdvrieze.formats.xpath.impl.NodeKindTest
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.AnyType
import io.github.pdvrieze.xml.schematypes.types.BooleanType
import io.github.pdvrieze.xml.schematypes.values.XsdBoolean
import io.github.pdvrieze.xml.schematypes.values.XsdInteger
import nl.adaptivity.xmlutil.dom2.ownerDocument

@XPathInternal
interface BuiltinFunction <out R: XdmValue> {
    val argumentTypes: List<XdmType>
    val returnType: XdmType

    context(ctx: ExprEvalContext)
    fun eval(vararg args: XdmValue): R = eval(args.toList())

    context(ctx: ExprEvalContext)
    fun eval(args: List<XdmValue>): R

    companion object {
        const val FN_NAMESPACE: String = "http://www.w3.org/2005/xpath-functions"
        const val MAP_NAMESPACE: String = "http://www.w3.org/2005/xpath-functions/map"
        const val ARRAY_NAMESPACE: String = "http://www.w3.org/2005/xpath-functions/array"
        const val MATH_NAMESPACE: String = "http://www.w3.org/2005/xpath-functions/math"


        fun <R: XdmValue> builtIn(name: String, returnType: AnyType,
                    vararg argumentTypes: AnyType,
                    evalFunction: context(ExprEvalContext) (List<XdmValue>) -> R): BuiltinFunction<R> {
            return BuiltinFunctionImpl(name, returnType, *argumentTypes, evalFunction = evalFunction)
        }

        fun <R: XdmValue> builtIn(
            name: String,
            returnType: XdmType,
            vararg argumentTypes: XdmType,
            evalFunction: context(ExprEvalContext) (List<XdmValue>) -> R
        ): BuiltinFunction<R> {
            return BuiltinFunctionImpl(name, returnType, argumentTypes.toList(), evalFunction = evalFunction)
        }

    }

    object FN {
        fun of(localName: String): BuiltinFunction<XdmValue>? = functions[localName]

        val FALSE = builtIn("false", BooleanType.Instance) { XdmAtomic(XsdBoolean.FALSE) }

        val TRUE = builtIn("true", BooleanType.Instance) { XdmAtomic(XsdBoolean.TRUE) }
        val DATA = builtIn("data", XdmSequenceType.ANY, XdmSequenceType.ANY) { it.singleArg<XdmValue>().atomize() }

        val BOOLEAN = builtIn("boolean", XdmSequenceType.boolean, XdmSequenceType.ANY) {
            XdmAtomic(XsdBoolean(it.singleArg<XdmValue>().toBoolean()))
        }

        private val functions = hashMapOf(
            "boolean" to BOOLEAN,
            "false" to FALSE,
            "true" to TRUE,
            "data" to DATA,
            "count" to builtIn("count", XdmSequenceType.integer, XdmSequenceType.ANY) {
                val arg = it.singleArg<XdmValue>()
                XdmAtomic(XsdInteger(arg.size.toLong()))
            },
            "root" to builtIn("root", XdmTypeTest(NodeKindTest.DocumentTest()), XdmSequenceType.node) { args ->
                val node = when {
                    args.isEmpty() -> contextOf<ExprEvalContext>().contextItem as XdmNode
                    else -> args.singleArg<XdmNode>()
                }
                XdmNode(node.node.ownerDocument)
            },
        )

    }
}

