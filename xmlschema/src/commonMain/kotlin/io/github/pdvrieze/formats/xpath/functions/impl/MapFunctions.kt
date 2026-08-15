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
import io.github.pdvrieze.formats.xpath.eval.type.XdmMapType
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.functions.argN
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

        val options = (args.getOrNull(1) as XdmMap?)?.content
        val duplicates = options?.get(XdmAtomic(XsdString("duplicates")))
            ?.let {
                DuplicateHandling.fromString(((it as XdmAtomic<*>).value as XsdString).xmlString)
                    ?: throw EvaluationException(ErrorCodes.FOJS0005, "Invalid duplicate handling value: $it")
            }
            ?: DuplicateHandling.USE_FIRST

        when (maps.size) {
            0 -> return@Fn XdmMap(emptyMap(), XdmMapType(ATOMIC.single, ITEM.any))
            1 -> return@Fn maps as XdmMap
        }

        val mapIt = maps.iterator()

        val resultMap = mapIt.next().content.toMutableMap()

        while (mapIt.hasNext()) {
            for ((key, value) in mapIt.next().content.entries) {
                when (duplicates) {
                    DuplicateHandling.REJECT -> if(resultMap.put(key, value)!=null) throw EvaluationException(ErrorCodes.FOJS0003, "Duplicate key $key already exists")
                    DuplicateHandling.USE_FIRST -> resultMap.put(key, value)?.let { resultMap.put(key, it) } // revert back to original
                    DuplicateHandling.USE_LAST,
                    DuplicateHandling.USE_ANY -> resultMap[key] = value
                    DuplicateHandling.COMBINE -> resultMap.put(key, value)?.let {
                        val combined = XdmSequence.build<XdmSingleValue<*>> {
                            add(it)
                            add(value)
                        }
                        resultMap.put(key, combined)
                    }
                }
            }
        }


        XdmMap(resultMap, maps[0].staticType)
    }

    val fnSize = BuiltinFunctionImpl.Map(
        "size",
        INTEGER.single, ANYMAP.single
    ) Fn@{ args ->
        val map = args[0].single() as XdmMap
        atomic(map.size)
    }

    val fnKeys = BuiltinFunctionImpl.Map(
        "keys",
        ATOMIC.any, ANYMAP.single
    ) Fn@{ args ->
        val map = args[0].single() as XdmMap
        XdmSequence.build<XdmAtomic<*>> {
            addAll(map.content.keys)
        }
    }

    val fnContains = BuiltinFunctionImpl.Map(
        "contains",
        BOOLEAN.single, ANYMAP.single, ATOMIC.single
    ) Fn@{ args ->
        val map = args[0].single() as XdmMap
        val key = args.argN<XdmAtomic<*>>(1)

        atomic(map.content.containsKey(key))
    }

    val fnGet = BuiltinFunctionImpl.Map(
        "get",
        ITEM.any, ANYMAP.single, ATOMIC.single
    ) Fn@{ args ->
        val map = args[0].single() as XdmMap
        val key = args.argN<XdmAtomic<*>>(1)

        map.content.get(key) ?: XdmSequence.EMPTY
    }

    private fun findImpl(collector: MutableList<XdmValue<*>>, input: XdmValue<*>, key: XdmAtomic<*>) {
        for (i in input) {
            when (i) {
                is XdmMap -> for ((k, v) in i.content) {
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
        val key = args.argN<XdmAtomic<*>>(1)
        val result = mutableListOf<XdmValue<*>>()

        findImpl(result, input, key)

        XdmArray(result, XdmArrayType(ITEM.any), XdmArrayType(ITEM.any))
    }

    val fnPut = BuiltinFunctionImpl.Map(
        "put",
        ANYMAP.single, ANYMAP.single, ATOMIC.single, ITEM.any
    ) Fn@{ args ->
        val map = args.argN<XdmMap>(0)
        val key = args.argN<XdmAtomic<*>>(1)
        val value = args.argN<XdmValue<*>>(2)

        val newContent = map.content.toMutableMap()
        newContent[key] = value

        XdmMap(newContent, map.staticType)
    }

    val fnEntry = BuiltinFunctionImpl.Map(
        "entry",
        ANYMAP.single, ATOMIC.single, ITEM.any
    ) Fn@{ args ->
        val key = args.argN<XdmAtomic<*>>(0)
        val value = args.argN<XdmValue<*>>(1)

        XdmMap(mapOf(key to value), XdmMapType(ATOMIC.single, ITEM.any))
    }

    val fnRemove = BuiltinFunctionImpl.Map(
        "remove",
        ANYMAP.single, ANYMAP.single, ATOMIC.single
    ) Fn@{ args ->
        val map = args[0].single() as XdmMap
        val key = args.argN<XdmAtomic<*>>(1)

        val changed = map.content.filterKeys { it != key }
        when {
            changed.size == map.content.size -> return@Fn map
            else -> return@Fn XdmMap(changed, map.staticType)
        }
    }

    enum class DuplicateHandling(val txt: String) {
        REJECT("reject"),
        USE_FIRST("use-first"),
        USE_LAST("use-last"),
        USE_ANY("use-any"),
        COMBINE("combine"),
        ;

        companion object {
            fun fromString(str: String): DuplicateHandling? =
                entries.firstOrNull { it.txt == str }
        }
    }

}
