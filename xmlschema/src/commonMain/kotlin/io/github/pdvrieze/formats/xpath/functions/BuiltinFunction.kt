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

import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmFunctionType
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath2
import io.github.pdvrieze.formats.xpath.impl.XPathInternal

@XPathInternal
interface BuiltinFunction<out R : XdmValue> : Function {
    override val functionTypes: List<XdmFunctionType>

    context(ctx: ExprEvalContext)
    override fun invoke(vararg args: XdmValue): R = invoke(args.toList())

    context(ctx: ExprEvalContext)
    override operator fun invoke(args: List<XdmValue>): R

    companion object {
        const val FN_NAMESPACE: String = "http://www.w3.org/2005/xpath-functions"
        const val MAP_NAMESPACE: String = "http://www.w3.org/2005/xpath-functions/map"
        const val ARRAY_NAMESPACE: String = "http://www.w3.org/2005/xpath-functions/array"
        const val MATH_NAMESPACE: String = "http://www.w3.org/2005/xpath-functions/math"
    }
}

