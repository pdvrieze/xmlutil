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

package io.github.pdvrieze.formats.xpath.data

import io.github.pdvrieze.formats.xpath.functions.BuiltinFunction
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal

@XPathInternal
class XdmBuiltinFunction(private val function: BuiltinFunction<*>): XdmFunction() {
    override val type: XdmSequenceType.Function =
        XdmSequenceType.Function(function.argumentTypes, function.returnType)

    context(ctx: ExprEvalContext)
    override fun withType(type: XdmType): XdmValue {
        TODO("Function casting not yet implemented")
    }

    override fun isValEqual(expected: XdmValue): Boolean {
        return expected is XdmBuiltinFunction && expected.function == function
    }

    override fun get(index: Int): XdmBuiltinFunction = this
}

