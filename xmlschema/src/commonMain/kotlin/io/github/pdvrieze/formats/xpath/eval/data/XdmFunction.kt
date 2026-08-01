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

package io.github.pdvrieze.formats.xpath.eval.data

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.type.XdmFunctionType
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal

@OptIn(XPathInternal::class)
sealed class XdmFunction<out T: XdmFunction<T>> : XdmSingleValue<T> {

    override abstract val staticType: XdmFunctionType
    abstract override val dynamicType: XdmFunctionType

    internal fun partialStaticType(args: List<XdmValue<*>?>): XdmFunctionType {
        return XdmFunctionType(args.indices.mapNotNull { idx ->
            if (args[idx] == null) staticType.argTypes[idx] else null
        }, staticType.returnType)
    }

    internal fun partialDynType(args: List<XdmValue<*>?>): XdmFunctionType {
        return XdmFunctionType(args.indices.mapNotNull { idx ->
            if (args[idx] == null) dynamicType.argTypes[idx] else null
        }, dynamicType.returnType)
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun atomizeTo(receiver: XdmSequence.XdmSequenceBuilder<XdmAtomic<*>>) {
        throw EvaluationException(ErrorCodes.FOTY0013, "Cannot atomize a function")
    }

    context(ctx: ExprEvalContext)
    override fun toBoolean(): Boolean {
        throw EvaluationException(FORG0006_INVALID_ARGUMENT_TYPE, "Cannot cast functions to boolean")
    }

    context(ctx: ExprEvalContext)
    abstract operator fun invoke(args: List<XdmValue<*>>): XdmValue<*>

    context(ctx: ExprEvalContext)
    operator fun invoke(vararg args: XdmValue<*>): XdmValue<*> = invoke(args.toList())

}

