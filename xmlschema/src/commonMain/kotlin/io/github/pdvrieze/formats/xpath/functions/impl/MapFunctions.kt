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
import io.github.pdvrieze.formats.xpath.eval.data.XdmMap.DuplicateHandling
import io.github.pdvrieze.formats.xpath.eval.type.XdmArrayType
import io.github.pdvrieze.formats.xpath.eval.type.XdmMapType
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.functions.xdmArg
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.values.XsdString

@XPathInternal
object MapFunctions : AbstractFunctionObject() {

    val fnMerge: BuiltinFunctionImpl<XdmMap> = BuiltinFunctionImpl.Map(
        "merge",
        listOf(
            functionType(ANYMAP.single, ANYMAP.any),
            functionType(ANYMAP.single, ANYMAP.any, ANYMAP.single),
        )
    ) Fn@{ args ->
        @Suppress("UNCHECKED_CAST")
        val maps = args[0] as XdmValue<XdmMap>

        val options = (args.getOrNull(1) as XdmMap?)
        val duplicates = options?.get(XdmAtomic(XsdString("duplicates")))
            ?.let {
                DuplicateHandling.fromString(((it as XdmAtomic<*>).value as XsdString).xmlString)
                    ?: throw EvaluationException(ErrorCodes.FOJS0005, "Invalid duplicate handling value: $it")
            }
            ?: DuplicateHandling.USE_FIRST

        when (maps.size) {
            0 -> XdmMap(emptyMap(), XdmMapType(ATOMIC.single, ITEM.any))
            1 -> maps as XdmMap
            else -> XdmMap.merge(maps, duplicates)
        }
    }

    val fnSize = BuiltinFunctionImpl.Map(
        "size",
        INTEGER.single, ANYMAP.single
    ) Fn@{ args ->
        val map = args.xdmArg<XdmMap>(0)
        atomic(map.size)
    }

    val fnKeys = BuiltinFunctionImpl.Map(
        "keys",
        ATOMIC.any, ANYMAP.single
    ) Fn@{ args ->
        val map = args.xdmArg<XdmMap>(0)
        XdmSequence.build<XdmAtomic<*>> {
            addAll(map.keys)
        }
    }

    val fnContains = BuiltinFunctionImpl.Map(
        "contains",
        BOOLEAN.single, ANYMAP.single, ATOMIC.single
    ) Fn@{ args ->
        val map = args.xdmArg<XdmMap>(0)
        val key = args.xdmArg<XdmAtomic<*>>(1)

        atomic(map.containsKey(key))
    }

    val fnGet = BuiltinFunctionImpl.Map(
        "get",
        ITEM.any, ANYMAP.single, ATOMIC.single
    ) Fn@{ args ->
        val map = args.xdmArg<XdmMap>(0)
        val key = args.xdmArg<XdmAtomic<*>>(1)

        map.get(key) ?: XdmSequence.EMPTY
    }

    private fun findImpl(collector: MutableList<XdmValue<*>>, input: XdmValue<*>, key: XdmAtomic<*>) {
        for (i in input) {
            when (i) {
                is XdmMap -> for ((k, v) in i.entries) {
                    if (k == key) collector.add(v)
                    findImpl(collector, v, key)
                }

                is XdmArray ->
                    for (j in i.content) findImpl(collector, j, key)

                else -> continue // ignore
            }
        }
    }

    val fnFind = BuiltinFunctionImpl.Map(
        "find",
        ITEM.any, ITEM.any, ATOMIC.single
    ) Fn@{ args ->
        val input = args[0]
        val key = args.xdmArg<XdmAtomic<*>>(1)
        val result = mutableListOf<XdmValue<*>>()

        findImpl(result, input, key)

        XdmArray(result, XdmArrayType(ITEM.any), XdmArrayType(ITEM.any))
    }

    val fnPut = BuiltinFunctionImpl.Map(
        "put",
        ANYMAP.single, ANYMAP.single, ATOMIC.single, ITEM.any
    ) Fn@{ args ->
        val map = args.xdmArg<XdmMap>(0)
        val key = args.xdmArg<XdmAtomic<*>>(1)
        val value = args.xdmArg<XdmValue<*>>(2)

        map.put(key, value)
    }

    val fnEntry = BuiltinFunctionImpl.Map(
        "entry",
        ANYMAP.single, ATOMIC.single, ITEM.any
    ) Fn@{ args ->
        val key = args.xdmArg<XdmAtomic<*>>(0)
        val value = args.xdmArg<XdmValue<*>>(1)

        XdmMap(mapOf(key to value), XdmMapType(ATOMIC.single, ITEM.any))
    }

    val fnRemove = BuiltinFunctionImpl.Map(
        "remove",
        ANYMAP.single, ANYMAP.single, ATOMIC.single
    ) Fn@{ args ->
        val map = args.xdmArg<XdmMap>(0)
        val keys = args.xdmArg<XdmValue<XdmAtomic<*>>>(1)

        return@Fn map.remove(keys)
    }


}
