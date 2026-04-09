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
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomicOrSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmSchemaType
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.functions.argN
import io.github.pdvrieze.formats.xpath.functions.atomicArgN
import io.github.pdvrieze.formats.xpath.functions.maybeCollation
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.DoubleType
import io.github.pdvrieze.xml.schematypes.types.FloatType
import io.github.pdvrieze.xml.schematypes.values.*
import kotlin.math.round

@XPathInternal
internal object SequenceFunctions : AbstractFunctionObject() {

    //region 14.1 General Functions and Operators on Sequences
    internal val fnEmpty = BuiltinFunctionImpl("empty", functionType(BOOLEAN, ITEM.any)) { args ->
        val arg = args.argOrContext() ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(XsdBoolean(arg.size==0))
    }

    internal val fnExists = BuiltinFunctionImpl("exists", functionType(BOOLEAN, ITEM.any)) { args ->
        val arg = args.argOrContext() ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(XsdBoolean(arg.size>0))
    }

    internal val fnHead = BuiltinFunctionImpl("head", functionType(ITEM.opt, ITEM.any)) { args ->
        val arg = args.argOrContext() ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        if (arg.size==0) return@BuiltinFunctionImpl XdmSequence.EMPTY
        arg[0]
    }

    internal val fnTail = BuiltinFunctionImpl("tail", functionType(ITEM.any, ITEM.any)) { args ->
        val arg = args.argOrContext() ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        if (arg !is XdmSequence<*> || arg.size<=1) return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmSequence.buildSingle(arg.staticType) {
            addAll(arg.asSequence().drop(1))
        }
    }

    internal val fnInsertBefore =
        BuiltinFunctionImpl("insert-before", functionType(ITEM.any, ITEM.any, INTEGER, ITEM.any)) { args ->
            if (args.size != 3) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
            val target = args[0]
            val position =
                ((args.atomicOrEmpty<XsdInteger>(1)
                    ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR)).toInt() - 1)
                    .coerceIn(0, target.size)
            val inserts = args[2]

            if (target.size == 0) return@BuiltinFunctionImpl inserts
            else if (inserts.size == 0) return@BuiltinFunctionImpl target

            XdmSequence.buildSingle(target.staticType) {
                for (i in 0 until position) {
                    this.add(target[i])
                }
                addAll(inserts)
                for (i in position until target.size) {
                    this.add(target[i])
                }
            }
        }

    internal val fnRemove = BuiltinFunctionImpl("remove", functionType(ITEM.any, ITEM.any, INTEGER)) { args ->
        if (args.size != 2) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        val target = args[0]
        val position = ((args.atomicOrEmpty<XsdInteger>(1) ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR)).toInt() -1)
        if (position < 0 || position>=target.size) return@BuiltinFunctionImpl target
        XdmSequence.buildSingle(target.staticType) {
            for (i in 0 until position) {
                add(target[i])
            }
            for (i in position+1 until target.size) {
                add(target[i])
            }
        }
    }

    internal val fnReverse = BuiltinFunctionImpl("reverse", functionType(ITEM.any, ITEM.any)) { args ->
        val arg = args.argOrContext() ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        if (arg.size==1) return@BuiltinFunctionImpl arg[0]

        XdmSequence.buildSingle(arg.staticType) {
            for(n in arg.size-1 downTo 0) { add(arg[n]) }
        }
    }

    internal val fnSubsequence = BuiltinFunctionImpl("subsequence", listOf(
        functionType(ITEM.any, ITEM.any, DOUBLE),
        functionType(ITEM.any, ITEM.any, DOUBLE, DOUBLE))
    ) { args ->
        if (args.size !in 2..3) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        val sourceSeq = args[0]
        val startingLocD = (args.atomicOrEmpty<XsdDouble>(1) ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR)).value
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

        XdmSequence.buildSingle {
            for (i in startingLoc until endLoc) {
                add(sourceSeq[i])
            }
        }
    }

    /**
     * Returns the same sequence as the argument. This function is only relevant for optimization.
     */
    internal val fnUnordered = BuiltinFunctionImpl("unordered", functionType(ITEM.any, ITEM.any)) { args ->
        args.argOrContext() ?: XdmSequence.EMPTY
    }
    //endregion

    //region 14.2 Sequence comparison functions
    val fnDistincValues = BuiltinFunctionImpl("distinct-values", listOf(
        functionType(ATOMIC.any, ATOMIC.any),
        functionType(ATOMIC.any, ATOMIC.any, STRING),
    )) { args ->
        val arg = args.argN<XdmValue<XdmAtomic<*>>>(0)
        val collation = args.maybeCollation(1)

        if (arg.size == 0) return@BuiltinFunctionImpl XdmSequence.EMPTY

        val distinct = HashSet<XdmAtomic<*>>()
        for(arg in args) { distinct.add(arg as XdmAtomic<*>) }

        val result = when (collation) {
            null -> distinct.toList()
            else -> distinct.sortedWith { l, r -> collation.compare(l.value.xmlString, r.value.xmlString) }
        }

        XdmSequence.fromList(result)
    }

    val fnIndexOf = BuiltinFunctionImpl("index-of", listOf(
        functionType(INTEGER, ATOMIC.any, ATOMIC),
        functionType(INTEGER, ATOMIC.any, ATOMIC, STRING),
    )) { args ->
        val seq = args.argN<XdmValue<XdmAtomic<*>>>(0) as XdmValue<XdmAtomic<*>>
        val search = args.atomicArgN<XsdAtomic>(1)
        val collation = args.maybeCollation(2)

        XdmSequence.buildAtomic {
            for (arg in seq) {
                val isEqual = when (collation) {
                    null -> arg.value == search
                    else -> collation.compare(arg.value.xmlString, search.xmlString) == 0
                }
                if (isEqual) this.add(arg)
            }
        }
    }
    val fnDeepEqual = BuiltinFunctionImpl("deep-equal", listOf(
        functionType(BOOLEAN, ITEM.any, ITEM.any),
        functionType(BOOLEAN, ITEM.any, ITEM.any, STRING),
    )) { args ->
        val param1 = args.argN<XdmValue<*>>(0)
        val param2 = args.argN<XdmValue<*>>(1)
        val collation = args.maybeCollation(2)

        when {
            param1.size != param2.size -> return@BuiltinFunctionImpl XdmAtomic(XsdBoolean.FALSE)
            param1.size == 0 -> return@BuiltinFunctionImpl XdmAtomic(XsdBoolean.TRUE)
        }

        for (i in 0 until param1.size) {
            val elem1 = param1[i]
            val elem2 = param1[i]
            if (! elem1.isDeepEqual(elem2, collation)) return@BuiltinFunctionImpl XdmAtomic(XsdBoolean.FALSE)
        }

        XdmAtomic(XsdBoolean.TRUE)
    }
    //endregion

    //region 14.3 Sequence cardinality testing functions
    internal val fnZeroOrOne = BuiltinFunctionImpl("zero-or-one", functionType(ITEM.opt, ITEM.any)) { args ->
        args[0].also { if (it.size > 1) throw EvaluationException(ErrorCodes.FORG0003) }
    }

    internal val fnOneOrMore = BuiltinFunctionImpl("one-or-more", functionType(ITEM.atLeastOne, ITEM.any)) { args ->
        args[0].also { if (it.size == 0) throw EvaluationException(ErrorCodes.FORG0004) }
    }

    internal val fnExactlyOne = BuiltinFunctionImpl("exactly-one", functionType(ITEM.single, ITEM.any)) { args ->
        args[0].also { if (it.size != 1) throw EvaluationException(ErrorCodes.FORG0005) }
    }

    //endregion

    //region 14.4 Sequence aggregate functions
    internal val fnCount = BuiltinFunctionImpl(
        "count",
        functionType(INTEGER, ITEM.any)
    ) { args ->
        XdmAtomic(XsdInteger(args[0].size))
    }

    context(ctx: ExprEvalContext)
    private fun seqSum(arg: XdmValue<XdmAtomic<*>>): XsdAtomic {
        val doubleCoerced = arg.map {
            when (it.staticType) {
                XdmSchemaType.UNTYPED_ATOMIC -> XsdDouble(it.value.xmlString)

                else -> it.value
            }
        }
        val head = doubleCoerced.first()
        val tail = doubleCoerced.asSequence().drop(1)

        return when (head) {
            is XsdDouble -> tail
                .map { it as? XsdDouble ?: throw EvaluationException(ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE)
                }.fold(head) { acc, d -> acc + d }

            is XsdFloat -> tail
                .map { it as? XsdFloat ?: throw EvaluationException(ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE)
                }.fold(head) { acc, d -> acc + d }

            is XsdDecimal -> tail
                .map { it as? XsdDecimal ?: throw EvaluationException(ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE)
                }.fold(head) { acc, d -> acc + d }

            is XsdYearMonthDuration -> tail
                .map { it as? XsdYearMonthDuration ?: throw EvaluationException(ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE)
                }.fold(head) { acc, d -> acc + d }

            is XsdDayTimeDuration -> tail
                .map { it as? XsdDayTimeDuration ?: throw EvaluationException(ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE)
                }.fold(head) { acc, d -> acc + d }

            else -> throw EvaluationException(ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE, "Average with unsupported type: ${head.schemaType}")
        }

    }


    internal val fnAvg = BuiltinFunctionImpl("avg", functionType(ATOMIC.opt, ATOMIC.any)) { args ->
        val arg = args[0]
        if (arg.size == 0) return@BuiltinFunctionImpl XdmSequence.EMPTY

        val sum = seqSum(arg as XdmAtomic<XsdAtomic>)
        val avg = when (sum) {
            is XsdDouble -> XsdDouble(sum.value / arg.size)
            is XsdFloat -> XsdFloat(sum.value / arg.size)
            is XsdDecimal -> sum / XsdInt(arg.size)
            is XsdYearMonthDuration -> sum / XsdDouble(arg.size.toDouble())
            is XsdDayTimeDuration -> sum / XsdDouble(arg.size.toDouble())
            else -> throw EvaluationException(ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE, "Average with unsupported type: ${sum.schemaType}")
        }
        XdmAtomic(avg)
    }

    context(context: ExprEvalContext)
    private fun getComparisonSequence(arg: XdmAtomicOrSequence<*>): List<XsdPrimitive> {
        @Suppress("UNCHECKED_CAST")
        val seq = arg.map {
            when ((it as XdmAtomic<*>).staticType) {
                XdmSchemaType.UNTYPED_ATOMIC -> XsdDouble(it.value.xmlString)

                // This is needed for comparison as the specification says to use the local timezone
                else if (it.value is IXsdDateTime) ->
                    it.value.ensureTimezone(contextOf<ExprEvalContext>().defaultTimeZone)

                else -> it.value
            } as XsdPrimitive
        }

        val usedTypes = seq.mapTo(HashSet()) { it.schemaType.primitiveType.name.localPart }

        val actualValues = when {
            usedTypes.size == 1 -> seq

            "string" in usedTypes && "anyURI" in usedTypes -> {
                if (usedTypes.size != 2) {
                    throw EvaluationException(
                        ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE,
                        "Max with unsupported types: ${usedTypes.joinToString()}"
                    )
                }
                seq.map { if (it is XsdAnyURI) XsdString(it.xmlString) else it }
            }

            "double" in usedTypes -> {
                if ((usedTypes - setOf("decimal", "float", "double")).isNotEmpty()) {
                    throw EvaluationException(
                        ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE,
                        "Max with unsupported types: ${usedTypes.joinToString()}"
                    )
                }
                seq.map { DoubleType.Instance.castFrom(it as XsdNumeric<*>) }
            }

            arrayOf("decimal", "float").any { it in usedTypes } -> {
                if ((usedTypes - setOf("decimal", "float", "double")).isNotEmpty()) {
                    throw EvaluationException(
                        ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE,
                        "Max with unsupported types: ${usedTypes.joinToString()}"
                    )
                }
                seq.map { FloatType.Instance.castFrom(it as XsdNumeric<*>) }
            }

            else -> throw EvaluationException(
                ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE,
                "Max with unsupported types: ${usedTypes.joinToString()}"
            )
        }
        return actualValues
    }

    internal val fnMax = BuiltinFunctionImpl<XdmAtomicOrSequence<*>>("max", listOf(
        functionType(ATOMIC.opt, ATOMIC.any),
        functionType(ATOMIC.opt, ATOMIC.any, STRING),
    )) { args ->
        val arg = args[0] as XdmAtomicOrSequence<*>
        if (arg.size == 0) return@BuiltinFunctionImpl XdmSequence.EMPTY
        val collation = args.maybeCollation(1) ?: contextOf<ExprEvalContext>().defaultCollation

        val actualValues = getComparisonSequence(arg)


        val max = actualValues.reduce { left, right ->
            if (left.compareTo(right, collation) > 0) left else right
        }
        XdmAtomic(max)
    }

    internal val fnMin = BuiltinFunctionImpl<XdmAtomicOrSequence<*>>("min", listOf(
        functionType(ATOMIC.opt, ATOMIC.any),
        functionType(ATOMIC.opt, ATOMIC.any, STRING),
    )) { args ->
        val arg = args[0] as XdmAtomicOrSequence<*>
        if (arg.size == 0) return@BuiltinFunctionImpl XdmSequence.EMPTY
        val collation = args.maybeCollation(1) ?: contextOf<ExprEvalContext>().defaultCollation

        val actualValues = getComparisonSequence(arg)


        val min = actualValues.reduce { left, right ->
            if (left.compareTo(right, collation) < 0) left else right
        }
        XdmAtomic(min)
    }

    internal val fnSum = BuiltinFunctionImpl("sum", listOf(
        functionType(ATOMIC.single, ATOMIC.any),
        functionType(ATOMIC.opt, ATOMIC.any, ATOMIC.opt),
    )) { args ->
        val arg = args[0]
        val zero = args.getOrNull(1)
        if (arg.size == 0) return@BuiltinFunctionImpl zero ?: XdmAtomic(XsdInt(0))

        XdmAtomic(seqSum(arg as XdmAtomic<XsdAtomic>))
    }


    //endregion

    //region 14.5 Functions on node identifiers
    //endregion

    //region 14.6 Functions giving access to external information
    //endregion

    //region 14.7 Parsing and serializing
    //endregion

}
