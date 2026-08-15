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

package io.github.pdvrieze.formats.xpath.functions

import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmFunctionType
import io.github.pdvrieze.formats.xpath.functions.impl.ArrayFunctions
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart

@OptIn(XPathInternal::class)
enum class ArrayFn(
    localName: String? = null,
    override val functionTypes: List<XdmFunctionType>,
    val implementation: context(ExprEvalContext) (List<XdmValue<*>>) -> XdmValue<*>,
) : XFunction<XdmValue<*>> {
    size(ArrayFunctions.fnSize),
    get(ArrayFunctions.fnGet),
    put(ArrayFunctions.fnPut),
    append(ArrayFunctions.fnAppend),
    subArray(ArrayFunctions.fnSubArray),
    remove(ArrayFunctions.fnRemove),
    insertBefore(ArrayFunctions.fnInsertBefore),
    head(ArrayFunctions.fnHead),
    tail(ArrayFunctions.fnTail),
    reverse(ArrayFunctions.fnReverse),
    join(ArrayFunctions.fnJoin),
    forEach(ArrayFunctions.fnForEach),
    filter(ArrayFunctions.fnFilter),
    foldLeft(ArrayFunctions.fnFoldLeft),
    foldRight(ArrayFunctions.fnFoldRight),
    forEachPair(ArrayFunctions.fnForEachPair),
    sort(ArrayFunctions.fnSort),
    flatten(ArrayFunctions.fnFlatten),
    ;

    override val functionName: QName = QName(BuiltinFunction.ARRAY_NAMESPACE, localName ?: name)

    constructor(builtinFunction: BuiltinFunctionImpl<XdmValue<*>>) :
            this(builtinFunction.functionName.localPart, builtinFunction.functionTypes, builtinFunction.evalFunction)

    constructor(
        types: List<XdmFunctionType>,
        implementation: context(ExprEvalContext) (List<XdmValue<*>>) -> XdmValue<*>,
    ): this(null, types, implementation)

    constructor(type: XdmFunctionType, implementation: context(ExprEvalContext) (List<XdmValue<*>>) -> XdmValue<*>):
            this(null, listOf(type), implementation)

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun invoke(args: List<XdmValue<*>>): XdmValue<*> {
        return implementation(args)
    }

    companion object {
        private val functionMap = entries.groupBy { it.functionName.localPart }
            .mapValues { (k, v) -> v.singleOrNull() ?: error("Multiple functions with name $k") }

        fun of(localName: String): ArrayFn? = functionMap[localName]
    }
}
