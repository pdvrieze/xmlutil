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
import io.github.pdvrieze.formats.xpath.eval.ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE
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
import io.github.pdvrieze.xml.schematypes.values.instances.XsdBigDecimal
import kotlin.math.round

@XPathInternal
internal object SequenceFunctions : AbstractFunctionObject() {

    //region 14.1 General Functions and Operators on Sequences
    internal val fnEmpty = BuiltinFunctionImpl.Fn("empty", BOOLEAN, ITEM.any) Fn@{ args ->
        val arg = args.argOrContext() ?: return@Fn XdmSequence.EMPTY
        atomic(arg.isEmpty())
    }

    internal val fnExists = BuiltinFunctionImpl.Fn("exists", BOOLEAN, ITEM.any) Fn@{ args ->
        val arg = args.argOrContext() ?: return@Fn XdmSequence.EMPTY
        atomic(arg.isNotEmpty())
    }

    internal val fnHead = BuiltinFunctionImpl.Fn("head", ITEM.opt, ITEM.any) Fn@{ args ->
        val arg = args.argOrContext() ?: return@Fn XdmSequence.EMPTY
        if (arg.isEmpty()) return@Fn XdmSequence.EMPTY
        arg[0]
    }

    internal val fnTail = BuiltinFunctionImpl.Fn("tail", ITEM.any, ITEM.any) Fn@{ args ->
        val arg = args.argOrContext() ?: return@Fn XdmSequence.EMPTY
        if (arg !is XdmSequence<*> || arg.size<=1) return@Fn XdmSequence.EMPTY
        XdmSequence.buildSingle(arg.staticType) {
            addAll(arg.asSequence().drop(1))
        }
    }

    internal val fnInsertBefore =
        BuiltinFunctionImpl.Fn("insert-before", ITEM.any, ITEM.any, INTEGER, ITEM.any) Fn@{ args ->
            if (args.size != 3) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
            val target = args[0]
            val position =
                ((args.atomicOrEmpty<XsdInteger>(1)
                    ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR)).toInt() - 1)
                    .coerceIn(0, target.size)
            val inserts = args[2]

            if (target.isEmpty()) return@Fn inserts
            else if (inserts.isEmpty()) return@Fn target

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

    internal val fnRemove = BuiltinFunctionImpl.Fn("remove", ITEM.any, ITEM.any, INTEGER) Fn@{ args ->
        if (args.size != 2) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        val target = args[0]
        val position = ((args.atomicOrEmpty<XsdInteger>(1) ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR)).toInt() -1)
        if (position < 0 || position >= target.size) return@Fn target
        XdmSequence.buildSingle(target.staticType) {
            for (i in 0 until position) {
                add(target[i])
            }
            for (i in position+1 until target.size) {
                add(target[i])
            }
        }
    }

    internal val fnReverse = BuiltinFunctionImpl.Fn("reverse", ITEM.any, ITEM.any) Fn@{ args ->
        val arg = args.argOrContext() ?: return@Fn XdmSequence.EMPTY
        if (arg.size == 1) return@Fn arg[0]

        XdmSequence.buildSingle(arg.staticType) {
            for (n in arg.size - 1 downTo 0) add(arg[n])
        }
    }

    internal val fnSubsequence = BuiltinFunctionImpl.Fn("subsequence", listOf(
        functionType(ITEM.any, ITEM.any, DOUBLE),
        functionType(ITEM.any, ITEM.any, DOUBLE, DOUBLE))
    ) Fn@{ args ->
        if (args.size !in 2..3) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        val sourceSeq = args[0]
        val startingLocD = (args.atomicOrEmpty<XsdDouble>(1) ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR)).value
        val endLocD = args.getOrNull(2)?.let {
            ((it as? XdmAtomic<*>)?.value as? XsdDouble)?.value ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Expected xs:double, found: ${it.staticType}")
        }
        if (startingLocD.isNaN() || startingLocD == Double.POSITIVE_INFINITY) return@Fn XdmSequence.EMPTY
        val startingLoc = (round(startingLocD).toInt() - 1).coerceIn(0, sourceSeq.size)

        val endLoc: Int
        if (endLocD != null) {
            if (endLocD.isNaN() || startingLocD == Double.NEGATIVE_INFINITY) return@Fn XdmSequence.EMPTY
            if (startingLocD == Double.NEGATIVE_INFINITY && endLocD == Double.POSITIVE_INFINITY) {
                return@Fn XdmSequence.EMPTY
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
    internal val fnUnordered = BuiltinFunctionImpl.Fn("unordered", ITEM.any, ITEM.any) Fn@{ args ->
        args.argOrContext() ?: XdmSequence.EMPTY
    }
    //endregion

    //region 14.2 Sequence comparison functions
    val fnDistinctValues = BuiltinFunctionImpl.Fn("distinct-values", listOf(
        functionType(ATOMIC.any, ATOMIC.any),
        functionType(ATOMIC.any, ATOMIC.any, STRING),
    )) Fn@{ args ->
        val arg = args.argN<XdmSequence<XdmAtomic<*>>>(0)
        val collation = args.maybeCollation(1)

        if (arg.isEmpty()) return@Fn XdmSequence.EMPTY

        val distinct = arg.toHashSet()

        val result = when (collation) {
            // null -> distinct.toList()
            else -> distinct.sortedWith { l, r -> collation.compare(l.value.xmlString, r.value.xmlString) }
        }

        XdmSequence.fromList(result, arg.staticType)
    }

    val fnIndexOf = BuiltinFunctionImpl.Fn("index-of", listOf(
        functionType(INTEGER, ATOMIC.any, ATOMIC),
        functionType(INTEGER, ATOMIC.any, ATOMIC, STRING),
    )) Fn@{ args ->
        val seq = args.argN<XdmValue<XdmAtomic<*>>>(0) as XdmValue<XdmAtomic<*>>
        val search = args.atomicArgN<XsdAtomic>(1)
        val collation = args.maybeCollation(2)

        XdmSequence.buildAtomic {
            for (arg in seq) {
                val isEqual = when (collation) {
                    // null -> arg.value == search
                    else -> collation.compare(arg.value.xmlString, search.xmlString) == 0
                }
                if (isEqual) this.add(arg)
            }
        }
    }
    val fnDeepEqual = BuiltinFunctionImpl.Fn("deep-equal", listOf(
        functionType(BOOLEAN, ITEM.any, ITEM.any),
        functionType(BOOLEAN, ITEM.any, ITEM.any, STRING),
    )) Fn@{ args ->
        val param1 = args.argN<XdmValue<*>>(0)
        val param2 = args.argN<XdmValue<*>>(1)
        val collation = args.maybeCollation(2)

        when {
            param1.size != param2.size -> return@Fn atomic(false)
            param1.size == 0 -> return@Fn atomic(true)
        }

        for (i in 0 until param1.size) {
            val elem1 = param1[i]
            val elem2 = param1[i]
            if (! elem1.isDeepEqual(elem2, collation)) return@Fn atomic(false)
        }

        atomic(true)
    }
    //endregion

    //region 14.3 Sequence cardinality testing functions
    internal val fnZeroOrOne = BuiltinFunctionImpl.Fn("zero-or-one", ITEM.opt, ITEM.any) Fn@{ args ->
        args[0].also { if (it.size > 1) throw EvaluationException(ErrorCodes.FORG0003) }
    }

    internal val fnOneOrMore = BuiltinFunctionImpl.Fn("one-or-more", ITEM.atLeastOne, ITEM.any) Fn@{ args ->
        args[0].also { if (it.size == 0) throw EvaluationException(ErrorCodes.FORG0004) }
    }

    internal val fnExactlyOne = BuiltinFunctionImpl.Fn("exactly-one", ITEM.single, ITEM.any) Fn@{ args ->
        args[0].also { if (it.size != 1) throw EvaluationException(ErrorCodes.FORG0005) }
    }

    //endregion

    //region 14.4 Sequence aggregate functions
    internal val fnCount = BuiltinFunctionImpl.Fn("count", INTEGER, ITEM.any) Fn@{ args ->
        atomic(args[0].size)
    }

    context(ctx: ExprEvalContext)
    private fun unifyNumericTypes(arg: XdmValue<XdmAtomic<*>>): List<XsdAtomic> {
        val result = ArrayList<XsdAtomic>(arg.size)
        var seenDouble = false
        var seenFloat = false
        var seenDecimal = false

        for (i in arg.indices) {
            val a = arg[i]
            val st = a.staticType
            val v = a.value
            when {
                st == XdmSchemaType.UNTYPED_ATOMIC -> {
                    try {
                        result.add(XsdDouble(v.xmlString))
                    } catch (e: NumberFormatException) {
                        throw EvaluationException(ErrorCodes.FORG0001, e)
                    }
                    seenDouble = true
                }

                v is XsdDouble -> {
                    result.add(v)
                    seenDouble = true
                }


                v is XsdFloat -> when {
                    seenDouble -> result.add(XsdDouble(v.value.toDouble()))
                    else -> {
                        result.add(v)
                        seenFloat = true
                    }
                }

                v is XsdDecimal -> when {
                    seenDouble -> result.add(XsdDouble(v.toBigDecimal().toDouble()))
                    seenFloat -> result.add(XsdFloat(v.toBigDecimal().toFloat()))

                    else -> {
                        result.add(v)
                        seenDecimal = true
                    }
                }

                v is XsdDuration -> result.add(v)

                else -> throw EvaluationException(FORG0006_INVALID_ARGUMENT_TYPE, "numeric sequence function with unsupported type: ${st}")
            }

            when {
                seenDouble -> if (seenDecimal || seenFloat) {
                    for (i in 0..<i) {
                        val v = result[i]
                        if (v !is XsdDouble) {
                            val a = arg[i].value
                            if (a is XsdNumeric<*>) result[i] = XsdDouble(a.toDouble())
                        }
                    }
                    seenFloat = false
                    seenDecimal = false
                }

                seenFloat && seenDecimal -> {
                    for (i in 0..<i) {
                        val v = result[i]
                        if (v is XsdDecimal) result[i] = XsdFloat(v.toFloat())
                    }
                    seenDecimal = false
                }
            }

        }
        return result
    }

    context(ctx: ExprEvalContext)
    private fun seqSum(arg: XdmValue<XdmAtomic<*>>): XsdAtomic {

        val doubleCoerced = unifyNumericTypes(arg)
        val head = doubleCoerced.first()
        val tail = doubleCoerced.asSequence().drop(1)

        return when (head) {
            is XsdDouble -> tail
                .map { it as? XsdDouble ?: throw EvaluationException(FORG0006_INVALID_ARGUMENT_TYPE)
                }.fold(head) { acc, d -> acc + d }

            is XsdFloat -> tail
                .map { it as? XsdFloat ?: throw EvaluationException(FORG0006_INVALID_ARGUMENT_TYPE)
                }.fold(head) { acc, d -> acc + d }

            is XsdDecimal -> tail.map {
                when (it) { // do conversion to large types
                    !is XsdDecimal -> throw EvaluationException(FORG0006_INVALID_ARGUMENT_TYPE, "Expected decimal, but was ${it.schemaType} ")
                    is XsdLong -> BigInt(it)
                    is XsdUnsignedLong -> BigUnsignedInt(it)
                    else -> it
                }
            }.fold(head) { acc, d ->
                acc + d
            }

            is XsdYearMonthDuration -> tail
                .map { it as? XsdYearMonthDuration ?: throw EvaluationException(FORG0006_INVALID_ARGUMENT_TYPE)
                }.fold(head) { acc, d -> acc + d }

            is XsdDayTimeDuration -> tail
                .map { it as? XsdDayTimeDuration ?: throw EvaluationException(FORG0006_INVALID_ARGUMENT_TYPE)
                }.fold(head) { acc, d -> acc + d }

            else -> throw EvaluationException(FORG0006_INVALID_ARGUMENT_TYPE, "Average with unsupported type: ${head.schemaType}")
        }

    }


    internal val fnAvg = BuiltinFunctionImpl.Fn("avg", functionType(ATOMIC.opt, ATOMIC.any)) Fn@{ args ->
        @Suppress("UNCHECKED_CAST")
        val arg = args[0] as XdmAtomicOrSequence<XdmAtomic<XsdAtomic>>
        if (arg.size == 0) return@Fn XdmSequence.EMPTY

        when (val sum = seqSum(arg)) {
            is XsdDouble -> atomic(sum.value / arg.size)
            is XsdFloat -> atomic(sum.value / arg.size)
            is XsdDecimal -> atomic(sum.toBigDecimal() / XsdBigDecimal(arg.size)) // division gives decimal
            is XsdYearMonthDuration -> atomic(sum / XsdDouble(arg.size.toDouble()))
            is XsdDayTimeDuration -> atomic(sum / XsdDouble(arg.size.toDouble()))
            else -> throw EvaluationException(FORG0006_INVALID_ARGUMENT_TYPE, "Average with unsupported type: ${sum.schemaType}")
        }
    }

    context(context: ExprEvalContext)
    private fun getComparisonSequence(arg: XdmAtomicOrSequence<*>): List<XsdPrimitive> {
        @Suppress("UNCHECKED_CAST")
        val seq = arg.toList().map {
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
                        FORG0006_INVALID_ARGUMENT_TYPE,
                        "Max with unsupported types: ${usedTypes.joinToString()}"
                    )
                }
                seq.map { if (it is XsdAnyURI) XsdString(it.xmlString) else it }
            }

            "double" in usedTypes -> {
                if ((usedTypes - setOf("decimal", "float", "double")).isNotEmpty()) {
                    throw EvaluationException(
                        FORG0006_INVALID_ARGUMENT_TYPE,
                        "Max with unsupported types: ${usedTypes.joinToString()}"
                    )
                }
                seq.map { DoubleType.Instance.castFrom(it as XsdNumeric<*>) }
            }

            arrayOf("decimal", "float").any { it in usedTypes } -> {
                if ((usedTypes - setOf("decimal", "float", "double")).isNotEmpty()) {
                    throw EvaluationException(
                        FORG0006_INVALID_ARGUMENT_TYPE,
                        "Max with unsupported types: ${usedTypes.joinToString()}"
                    )
                }
                seq.map { FloatType.Instance.castFrom(it as XsdNumeric<*>) }
            }

            else -> throw EvaluationException(
                FORG0006_INVALID_ARGUMENT_TYPE,
                "Max with unsupported types: ${usedTypes.joinToString()}"
            )
        }
        return actualValues
    }

    internal val fnMax = BuiltinFunctionImpl.Fn(
        "max", listOf(
            functionType(ATOMIC.opt, ATOMIC.any),
            functionType(ATOMIC.opt, ATOMIC.any, STRING),
        ), Fn@{ args ->
            val arg = args[0] as XdmAtomicOrSequence<*>
            if (arg.size == 0) return@Fn XdmSequence.EMPTY
            val collation = args.maybeCollation(1)

            val actualValues = getComparisonSequence(arg)


            val max = actualValues.reduce { left, right ->
                if (left.compareTo(right, collation) > 0) left else right
            }
            atomic(max)
        })

    internal val fnMin = BuiltinFunctionImpl.Fn(
        "min", listOf(
            functionType(ATOMIC.opt, ATOMIC.any),
            functionType(ATOMIC.opt, ATOMIC.any, STRING),
        ), Fn@{ args ->
            val arg = args[0] as XdmAtomicOrSequence<*>
            if (arg.isEmpty()) return@Fn XdmSequence.EMPTY
            val collation = args.maybeCollation(1)

            val actualValues = getComparisonSequence(arg)


            val min = actualValues.reduce { left, right ->
                if (left.compareTo(right, collation) < 0) left else right
            }
            atomic(min)
        })

    internal val fnSum = BuiltinFunctionImpl.Fn("sum", listOf(
        functionType(ATOMIC.single, ATOMIC.any),
        functionType(ATOMIC.opt, ATOMIC.any, ATOMIC.opt),
    )) Fn@{ args ->
        val arg = args[0]
        val zero = args.getOrNull(1)
        if (arg.isEmpty()) return@Fn zero ?: XdmAtomic(XsdInt(0))

        atomic(seqSum(arg as XdmAtomic<XsdAtomic>))
    }


    //endregion

    //region 14.5 Functions on node identifiers
    //endregion

    //region 14.6 Functions giving access to external information
    val fnEnvironmentVariable = BuiltinFunctionImpl.Fn("environment-variable", STRING.any, STRING.single) Fn@{ args ->
        val arg = args.atomicArgN<XsdString>(0).xmlString
        val ctx = contextOf<ExprEvalContext>()
        atomicOrNull(ctx.environmentVariables[arg])
    }

    val fnAvailableEnvironmentVariables = BuiltinFunctionImpl.Fn("available-environment-variables", STRING.any) { args ->
        val ctx = contextOf<ExprEvalContext>()
        XdmSequence.buildAtomic {
            for (key in ctx.environmentVariables.keys) {
                add(atomic(key))
            }
        }
    }

    val fnCollection = BuiltinFunctionImpl.Fn("collection", listOf(
        functionType(ITEM.any),
        functionType(ITEM.any, STRING.opt),
    )) { args ->
        val uri = when (args.size) {
            0 -> XsdAnyURI("")
            else -> args.atomicArgN<XsdString>(0).let { XsdAnyURI(it.xmlString) }
        }
        val ctx = contextOf<ExprEvalContext>()
        ctx.collections[uri] ?: throw EvaluationException(ErrorCodes.FODC0002, "Unknown collection: $uri")
    }

    //endregion

    //region 14.7 Parsing and serializing
    //endregion

}
