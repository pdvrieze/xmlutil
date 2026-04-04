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

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmBoolean
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.functions.XFunction
import io.github.pdvrieze.formats.xpath.functions.atomicArgN
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.BooleanType
import io.github.pdvrieze.xml.schematypes.values.XsdBoolean

@XPathInternal
object BooleanFunctions: AbstractFunctionObject() {
    val fnTrue: BuiltinFunctionImpl<XdmBoolean> = BuiltinFunctionImpl(
        "true",
        BooleanType.Instance
    ) { args ->
        if (args.isNotEmpty()) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        XdmAtomic(XsdBoolean.TRUE)
    }
    val fnFalse: BuiltinFunctionImpl<XdmBoolean> = BuiltinFunctionImpl(
        "false",
        BooleanType.Instance
    ) { args ->
        if (args.isNotEmpty()) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        XdmAtomic(XsdBoolean.FALSE)
    }

    val opBooleanEqual: BuiltinFunctionImpl<XdmBoolean> = BuiltinFunctionImpl("op:boolean-equal", BooleanType.Instance, BooleanType.Instance, BooleanType.Instance) { args ->
        if (args.size!=2) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        val left = args.atomicArgN<XsdBoolean>(0).value
        val right = args.atomicArgN<XsdBoolean>(1).value

        XdmAtomic(XsdBoolean(left == right))
    }

    val opBooleanLessThan: BuiltinFunctionImpl<XdmBoolean> = BuiltinFunctionImpl(
        "op:boolean-less-than",
        BooleanType.Instance,
        BooleanType.Instance,
        BooleanType.Instance
    ) { args ->
        if (args.size!=2) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        val left = args.atomicArgN<XsdBoolean>(0).value
        val right = args.atomicArgN<XsdBoolean>(1).value

        XdmAtomic(XsdBoolean(!left && right))
    }

    val opBooleanGreaterThan: BuiltinFunctionImpl<XdmBoolean> = BuiltinFunctionImpl(
        "op:boolean-greater-than",
        BooleanType.Instance,
        BooleanType.Instance,
        BooleanType.Instance
    ) { args ->
        if (args.size != 2) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        val left = args.atomicArgN<XsdBoolean>(0).value
        val right = args.atomicArgN<XsdBoolean>(1).value

        XdmAtomic(XsdBoolean(left && !right))
    }

    val fnBoolean: BuiltinFunctionImpl<XdmBoolean> = BuiltinFunctionImpl(
        "boolean",
        listOf(functionType(BooleanType.Instance, ITEM.any))
    ) { args ->
            val r = toSingleArg(args)?.toBoolean() ?: false
            XdmAtomic(XsdBoolean(r))
        }

    val fnNot: BuiltinFunctionImpl<XdmBoolean> = BuiltinFunctionImpl(
        "boolean",
        listOf(functionType(BooleanType.Instance, ITEM.any))
    ) { args ->
            val r = toSingleArg(args)?.toBoolean() ?: false
            XdmAtomic(XsdBoolean(!r))
        }
}
