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
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.values.XsdBoolean
import io.github.pdvrieze.xml.schematypes.values.XsdDouble
import io.github.pdvrieze.xml.schematypes.values.XsdInteger
import kotlin.math.round

@XPathInternal
internal object SequenceFunctions : AbstractFunctionObject() {

    internal val fnEmpty = BuiltinFunctionImpl("empty", functionType(BOOLEAN, ITEM.any)) { args ->
        val arg = toSingleArg(args) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(XsdBoolean(arg.size==0))
    }

    internal val fnExists = BuiltinFunctionImpl("exists", functionType(BOOLEAN, ITEM.any)) { args ->
        val arg = toSingleArg(args) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(XsdBoolean(arg.size>0))
    }

    internal val fnHead = BuiltinFunctionImpl("head", functionType(ITEM.opt, ITEM.any)) { args ->
        val arg = toSingleArg(args) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        if (arg.size==0) return@BuiltinFunctionImpl XdmSequence.EMPTY
        arg[0]
    }

    internal val fnTail = BuiltinFunctionImpl("tail", functionType(ITEM.any, ITEM.any)) { args ->
        val arg = toSingleArg(args) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        if (arg !is XdmSequence<*> || arg.size<=1) return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmSequence(arg.drop(1))
    }

    internal val fnInsertBefore = BuiltinFunctionImpl("insert-before", functionType(ITEM.any, ITEM.any, INTEGER, ITEM.any)) { args ->
        if (args.size != 3) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        val target = args[0]
        val position = ((toAtomic<XsdInteger>(1, args) ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR)).toInt() -1)
            .coerceIn(0, target.size)
        val inserts = args[2]

        if (target.size == 0) return@BuiltinFunctionImpl inserts
        else if (inserts.size == 0) return@BuiltinFunctionImpl target

        val elements = buildList {
            for (i in 0 until position) {
                add(target[i])
            }
            addAll(inserts)
            for (i in position until target.size) {
                add(target[i])
            }
        }

        XdmSequence(elements)
    }

    internal val fnRemove = BuiltinFunctionImpl("remove", functionType(ITEM.any, ITEM.any, INTEGER)) { args ->
        if (args.size != 2) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        val target = args[0]
        val position = ((toAtomic<XsdInteger>(1, args) ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR)).toInt() -1)
        if (position < 0 || position>=target.size) return@BuiltinFunctionImpl target
        val newElements = buildList {
            for (i in 0 until position) {
                add(target[i])
            }
            for (i in position+1 until target.size) {
                add(target[i])
            }
        }
        XdmSequence(newElements)
    }

    internal val fnReverse = BuiltinFunctionImpl("reverse", functionType(ITEM.any, ITEM.any)) { args ->
        val arg = toSingleArg(args) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        if (arg.size==1) return@BuiltinFunctionImpl arg[0]

        val reversed = (arg.size-1 downTo 0).map { arg[it] }
        XdmSequence(reversed)
    }

    internal val fnSubsequence = BuiltinFunctionImpl("subsequence", listOf(
        functionType(ITEM.any, ITEM.any, DOUBLE),
        functionType(ITEM.any, ITEM.any, DOUBLE, DOUBLE))
    ) { args ->
        if (args.size !in 2..3) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        val sourceSeq = args[0]
        val startingLocD = (toAtomic<XsdDouble>(1, args) ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR)).value
        val endLocD = args.getOrNull(2)?.let {
            ((it as? XdmAtomic<*>)?.value as? XsdDouble)?.value ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Expected xs:double, found: ${it.staticType}")
        }
        if (startingLocD.isNaN() || startingLocD == Double.POSITIVE_INFINITY) return@BuiltinFunctionImpl XdmSequence.EMPTY
        val startingLoc = (round(startingLocD).toInt() - 1).coerceIn(0, sourceSeq.size)

        val endLoc: Int
        if (endLocD != null) {
            if (endLocD.isNaN() || startingLocD == Double.NEGATIVE_INFINITY) return@BuiltinFunctionImpl XdmSequence.EMPTY
            if (startingLocD == Double.NEGATIVE_INFINITY && endLocD == Double.POSITIVE_INFINITY) {
                return@BuiltinFunctionImpl XdmSequence.EMPTY
            }
            endLoc = (startingLoc +round(endLocD).toInt()).coerceIn(0, sourceSeq.size)
        } else {
            endLoc = sourceSeq.size
        }

        val newElements = buildList {
            for (i in startingLoc until endLoc) {
                add(sourceSeq[i])
            }
        }

        when (newElements.size) {
            0 -> XdmSequence.EMPTY
            1 -> newElements.single()
            else -> XdmSequence(newElements)
        }
    }

    /**
     * Returns the same sequence as the argument. This function is only relevant for optimization.
     */
    internal val fnUnordered = BuiltinFunctionImpl("unordered", functionType(ITEM.any, ITEM.any)) { args ->
        toSingleArg(args) ?: XdmSequence.EMPTY
    }

    internal val fnCount = BuiltinFunctionImpl(
        "count",
        functionType(INTEGER, ITEM.any)
    ) { args ->
        val arg = toSingleArg(args) ?: return@BuiltinFunctionImpl XdmAtomic(XsdInteger(0))
        XdmAtomic(XsdInteger(arg.size))
    }


}
