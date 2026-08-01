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

package io.github.pdvrieze.formats.xpath.functions.impl

import io.github.pdvrieze.formats.xpath.eval.data.XdmArray
import io.github.pdvrieze.formats.xpath.eval.data.XdmBuiltinFunction
import io.github.pdvrieze.formats.xpath.eval.data.XdmFunction
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmArrayTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmFunctionTypeTest
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.functions.XFunction
import io.github.pdvrieze.formats.xpath.functions.atomicArgN
import io.github.pdvrieze.formats.xpath.functions.xdmArg
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath3_0
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.values.XsdInteger
import io.github.pdvrieze.xml.schematypes.values.XsdQName
import nl.adaptivity.xmlutil.QName

@XPathInternal
object HigherOrderFunctions : AbstractFunctionObject() {
    //region 16.1 Functions on functions
    @OptIn(NeedsXPath3_0::class)
    val fnFunctionLookup = BuiltinFunctionImpl(
        "function-lookup",
        XdmFunctionTypeTest.ANY_FUNCTION.opt,
        QNAME, INTEGER
    ) { args ->
        val ctx = contextOf<ExprEvalContext>()
        val name = args.atomicArgN<XsdQName>(0).toQName()
        val arity = args.atomicArgN<XsdInteger>(1).toInt()
        ctx.resolveFunction(name, arity) ?: XdmSequence.EMPTY
    }

    val fnFunctionName = BuiltinFunctionImpl("function-name", QNAME.opt, XdmFunctionTypeTest.ANY_FUNCTION.single) { args ->
        when(val fn = args.xdmArg<XdmFunction<*>>(0)) {
            is XdmBuiltinFunction -> atomic(XsdQName(fn.functionName))
            else -> XdmSequence.EMPTY
        }
    }

    val fnFunctionArity = BuiltinFunctionImpl("function-arity", INTEGER, XdmFunctionTypeTest.ANY_FUNCTION.single) { args ->
        val fn = args.xdmArg<XdmFunction<*>>(0)
        atomic(fn.staticType.argTypes.size)
    }

    //endregion

    //region 16.2 Basic higher order functions

    val fnForEach = BuiltinFunctionImpl("for-each", ITEM.any, ITEM.any, XdmFunctionTypeTest(ITEM.any, ITEM.single)) { args ->
        val seq = args[0]
        val action = args.xdmArg<XdmFunction<*>>(1)

        val results = seq.flatMap {
            action(it)
        }
        XdmSequence.fromList(results)
    }

    val fnApply = BuiltinFunctionImpl("apply", ITEM.any, XdmFunctionTypeTest.ANY_FUNCTION.single, XdmArrayTypeTest.ANY_ARRAY.single) { args ->
        val function = args.xdmArg<XdmFunction<*>>(0)
        val array = args.xdmArg<XdmArray>(1)
        val functionName = when (function) {
            is XdmBuiltinFunction -> function.functionName
            else -> QName("<anonymous>")
        }

        val promotedArgs = XFunction.promoteArguments(array.content, function.dynamicType, functionName)
        function(promotedArgs)
    }

    //endregion

    //region 16.3 Dynamic loading
    //endregion

}
