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
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSequenceTypeTest
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import nl.adaptivity.xmlutil.QName

interface XFunction<out R : XdmValue<*>> {
    val functionName: QName

    val functionTypes: List<XdmFunctionType>

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun getReturnType(): XdmSequenceTypeTest {
        return functionTypes.asSequence()
            .map { it.returnType }
            .reduce { acc, type -> acc.sharedBaseType(type) }
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    operator fun invoke(vararg args: XdmValue<*>): R = invoke(args.toList())

    @XPathInternal
    context(ctx: ExprEvalContext)
    operator fun invoke(args: List<XdmValue<*>>): R

}

