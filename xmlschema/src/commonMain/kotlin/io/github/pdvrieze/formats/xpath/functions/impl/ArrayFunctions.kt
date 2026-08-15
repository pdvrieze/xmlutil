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
import io.github.pdvrieze.formats.xpath.eval.data.*
import io.github.pdvrieze.formats.xpath.eval.type.XdmArrayType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmFunctionTypeTest
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.functions.XFunction
import io.github.pdvrieze.formats.xpath.functions.atomicArgN
import io.github.pdvrieze.formats.xpath.functions.xdmArg
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath3_0
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.values.XsdInteger
import io.github.pdvrieze.xml.schematypes.values.XsdString
import nl.adaptivity.xmlutil.QName

@XPathInternal
object ArrayFunctions : AbstractFunctionObject() {

    val fnSize = BuiltinFunctionImpl.Array(
        "size",
        INTEGER.single, ANYARRAY.single
    ) Fn@{ args ->
        val array = args.xdmArg<XdmArray>(0)
        atomic(array.size)
    }

    val fnGet = BuiltinFunctionImpl.Array(
        "get",
        ITEM.any, ANYARRAY.single, INTEGER.single
    ) Fn@{ args ->
        val array = args.xdmArg<XdmArray>(0)
        val position = args.atomicArgN<XsdInteger>(1).toInt()

        array.content.getOrElse(position) { throw EvaluationException(ErrorCodes.FOAY0001_ARRAY_BOUNDS) }
    }

    val fnPut = BuiltinFunctionImpl.Array(
        "put",
        ANYARRAY.single, ANYARRAY.single, INTEGER.single, ITEM.any
    ) Fn@{ args ->
        val array = args.xdmArg<XdmArray>(0)
        val position = args.atomicArgN<XsdInteger>(1).toInt()
        val value = args.xdmArg<XdmValue<*>>(2)

        val newData = array.content.toMutableList()
        newData[position-1] = value
        XdmArray(newData, array.staticType, array.dynamicType)
    }

    val fnAppend = BuiltinFunctionImpl.Array(
        "append",
        ANYARRAY.single, ANYARRAY.single, ITEM.any
    ) Fn@{ args ->
        val array = args.xdmArg<XdmArray>(0)
        val appendage = args[1]

        if (appendage.isEmpty()) return@Fn array

        val newData = array.content + appendage

        XdmArray(newData, array.staticType, array.dynamicType)
    }

    val fnSubArray = BuiltinFunctionImpl.Array(
        "subarray",
        listOf(
            functionType(ANYARRAY.single, ANYARRAY.single, INTEGER.any),
            functionType(ANYARRAY.single, ANYARRAY.single, INTEGER.single, INTEGER.single),
            )
    ) Fn@{ args ->
        val array = args.xdmArg<XdmArray>(0)
        val start = args.atomicArgN<XsdInteger>(1).toInt()
        val length= if(args.size == 3) args.atomicArgN<XsdInteger>(2).toInt() else array.size - start + 1
        if (length < 0) throw EvaluationException(ErrorCodes.FOAY0002_NEG_ARRAY_LENGTH)

        if (start == 1 && length == array.size) return@Fn array
        if (start + length > array.size + 1) throw EvaluationException(ErrorCodes.FOAY0001_ARRAY_BOUNDS)
        if (start > array.size) return@Fn XdmArray(emptyList(), array.staticType, array.dynamicType)

        val newData = array.content.subList(start - 1, start - 1 + length)

        XdmArray(newData, array.staticType, array.dynamicType)
    }

    val fnRemove = BuiltinFunctionImpl.Array(
        "remove",
        ANYARRAY.single, ANYARRAY.single, INTEGER.any
    ) Fn@{ args ->
        val array = args.xdmArg<XdmArray>(0)
        val positions = args[1].mapTo(HashSet()) {
            val pos = ((it as XdmAtomic<*>).value as XsdInteger).toInt() - 1
            if (pos !in array.content.indices) throw EvaluationException(ErrorCodes.FOAY0001_ARRAY_BOUNDS, "${pos + 1} !in 1..${array.content.size}")
            pos
        }

        val newData = array.content.filterIndexed { i, _ -> i !in positions }

        XdmArray(newData, array.staticType, array.dynamicType)
    }

    val fnInsertBefore = BuiltinFunctionImpl.Array(
        "insert-before",
        ANYARRAY.single, ANYARRAY.single, INTEGER.single, ITEM.any
    ) Fn@{ args ->
        val array = args.xdmArg<XdmArray>(0)
        val position = args.atomicArgN<XsdInteger>(1).toInt()
        val member = args.xdmArg<XdmValue<*>>(2)

        val newData = ArrayList<XdmValue<*>>(array.size + 1)
        if (position > 1) newData.addAll(array.content.subList(0, position - 1))
        newData.add(member)
        if (position - 1 < array.size) newData.addAll(array.content.subList(position-1, array.size))

        XdmArray(newData, array.staticType, array.dynamicType)
    }

    val fnHead = BuiltinFunctionImpl.Array(
        "head",
        ITEM.any, ANYARRAY.single
    ) Fn@{ args ->
        val array = args.xdmArg<XdmArray>(0)

        array.content.getOrElse(0) { throw EvaluationException(ErrorCodes.FOAY0001_ARRAY_BOUNDS) }
    }

    val fnTail = BuiltinFunctionImpl.Array("tail", ANYARRAY.single, ANYARRAY.single) Fn@{ args ->
        val array = args.xdmArg<XdmArray>(0)
        if (array.isEmpty()) throw EvaluationException(ErrorCodes.FOAY0001_ARRAY_BOUNDS)

        val newData = array.content.subList(1, array.content.size)

        XdmArray(newData, array.staticType, array.dynamicType)
    }

    val fnReverse = BuiltinFunctionImpl.Array("reverse", ANYARRAY.single, ANYARRAY.single) Fn@{ args ->
        val array = args.xdmArg<XdmArray>(0)
        if (array.isEmpty()) throw EvaluationException(ErrorCodes.FOAY0001_ARRAY_BOUNDS)

        val newData = array.content.reversed()

        XdmArray(newData, array.staticType, array.dynamicType)
    }

    val fnJoin = BuiltinFunctionImpl.Array("join", ANYARRAY.single, ANYARRAY.any) Fn@{ args ->
        val array = args[0]
        if (array.isEmpty()) {
            return@Fn XdmArray(emptyList(), XdmArrayType.ANY)
        }

        val newData = array.flatMap { (it as XdmArray).content }

        XdmArray(newData, XdmArrayType.ANY)
    }

    @OptIn(NeedsXPath3_0::class)
    val fnForEach = BuiltinFunctionImpl.Array("for-each", ANYARRAY.single, ANYARRAY.single, XdmFunctionTypeTest(ITEM.any, ITEM.any).single) Fn@{ args ->
        val array = args.xdmArg<XdmArray>(0)
        val function = args.xdmArg<XdmFunction<*>>(1)

        if (array.isEmpty()) throw EvaluationException(ErrorCodes.FOAY0001_ARRAY_BOUNDS)

        val newData = array.content.map { function(XFunction.promoteArguments(listOf(it), function.staticType, QName("<unnamed>"))) }

        XdmArray(newData, XdmArrayType(function.staticType.returnType))
    }

    val fnFilter = BuiltinFunctionImpl.Array(
        "filter",
        ANYARRAY.single, ANYARRAY.single, XdmFunctionTypeTest(BOOLEAN.single, ITEM.any)
    ) Fn@{ args ->
        val array = args.xdmArg<XdmArray>(0)
        val function = args.xdmArg<XdmFunction<*>>(1)
        if (array.isEmpty()) return@Fn array

        val newData = array.content.filter {
            function(XFunction.promoteArguments(listOf(it), function.staticType, QName("<unnamed>"))).toBoolean()
        }

        XdmArray(newData, array.staticType, array.dynamicType)
    }

    val fnFoldLeft = BuiltinFunctionImpl.Array(
        "fold-left",
        ITEM.any, ANYARRAY.single, ITEM.any, XdmFunctionTypeTest(ITEM.any, ITEM.any, ITEM.any)
    ) Fn@{ args ->
        val array = args.xdmArg<XdmArray>(0)
        var zero = args.xdmArg<XdmValue<*>>(1)
        val function = args.xdmArg<XdmFunction<*>>(2)
        if (array.isEmpty()) return@Fn zero

        var acc = zero
        for (i in array) {
            acc = function(XFunction.promoteArguments(listOf(acc, i), function.staticType, QName("<unnamed>")))
        }
        acc
    }

    val fnFoldRight = BuiltinFunctionImpl.Array(
        "fold-right",
        ITEM.any, ANYARRAY.single, ITEM.any, XdmFunctionTypeTest(ITEM.any, ITEM.any, ITEM.any)
    ) Fn@{ args ->
        val array = args.xdmArg<XdmArray>(0)
        var zero = args.xdmArg<XdmValue<*>>(1)
        val function = args.xdmArg<XdmFunction<*>>(2)
        if (array.isEmpty()) return@Fn zero

        var acc = zero
        for (i in array.reversed()) {
            acc = function(XFunction.promoteArguments(listOf(acc, i), function.staticType, QName("<unnamed>")))
        }
        acc
    }


    @OptIn(NeedsXPath3_0::class)
    val fnForEachPair = BuiltinFunctionImpl.Array("for-each-pair",
        ANYARRAY.single, ANYARRAY.single, ANYARRAY.single, XdmFunctionTypeTest(ITEM.any, ITEM.any, ITEM.any).single
    ) Fn@{ args ->
        val array1 = args.xdmArg<XdmArray>(0)
        val array2 = args.xdmArg<XdmArray>(1)
        val function = args.xdmArg<XdmFunction<*>>(2)

        val newData = (0 until minOf(array1.size, array2.size)).map {
            function(XFunction.promoteArguments(listOf(array1[it], array2[it]), function.staticType, QName("<unnamed>")))
        }

        XdmArray(newData, XdmArrayType(function.staticType.returnType))
    }


    val fnSort = BuiltinFunctionImpl.Array("sort", listOf(
        functionType(ANYARRAY.single, ANYARRAY.single),
        functionType(ANYARRAY.single, ANYARRAY.single, STRING.opt),
        functionType(ANYARRAY.single, ANYARRAY.single, STRING.opt, XdmFunctionTypeTest(ATOMIC.any, ITEM.any))
    )) Fn@{ args ->
        val array = args.xdmArg<XdmArray>(0)
        if (array.isEmpty()) throw EvaluationException(ErrorCodes.FOAY0001_ARRAY_BOUNDS)

        val ctx = contextOf<ExprEvalContext>()

        val collation = (if (args.size>1) args.atomicOrEmpty<XsdString>(1) else null)
            ?.let { ctx.collation(it.xmlString) ?: throw EvaluationException(ErrorCodes.FOCH0002, "Unsupported collation: ${it.xmlString}") }
            ?: ctx.defaultCollation

        val key = if (args.size == 3) args.xdmArg<XdmFunction<*>>(2) else null

        val keys: List<XdmAtomic<*>> = when (key) {
            null -> array.content.map { Accessors.fnData(it).first() }
            else -> array.content.map { key(XFunction.promoteArguments(listOf(it), key.staticType, QName("<unnamed>"))).first() as XdmAtomic<*> }
        }

        val sortedIndices = array.indices.sortedWith { i1, i2 ->
            val v1 = keys[i1].value.xmlString
            val v2 = keys[i2].value.xmlString
            collation.compare(v1, v2)
        }

        val newData = sortedIndices.map { array[it] }

        XdmArray(newData, array.staticType, array.dynamicType)
    }

    private fun flattenImpl(item: XdmValue<*>, receiver: XdmSequence.XdmSequenceBuilder<XdmSingleValue<*>>) {
        when (item) {
            is XdmArray -> item.content.forEach { flattenImpl(it, receiver) }
            !is XdmSingleValue<*> -> item.forEach { flattenImpl(it, receiver) }
            else -> receiver.add(item)
        }
    }

    val fnFlatten = BuiltinFunctionImpl.Array("flatten", ITEM.any, ITEM.any) Fn@{ args ->
        val items = args[0]
        if (items.isEmpty()) return@Fn XdmSequence.EMPTY

        XdmSequence.build<XdmSingleValue<*>> {
            flattenImpl(items, this)
        }
    }

}
