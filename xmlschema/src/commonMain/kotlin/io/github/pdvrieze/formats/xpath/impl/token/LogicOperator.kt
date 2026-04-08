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

package io.github.pdvrieze.formats.xpath.impl.token

import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomicOrEmpty
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.values.XsdBoolean

abstract class LogicOperator(
    literal: String,
    priority: Int,
    minVersion: XPathVersion = XPathVersion.XPath3_1,
    isDelimiting: Boolean,
) : Operator(literal, priority, minVersion, isDelimiting) {
    abstract operator fun invoke(left: Boolean, right: Boolean): Boolean

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun eval(left: XdmValue<*>, right: XdmValue<*>): XdmAtomicOrEmpty<XdmAtomic<XsdBoolean>> {
        val leftBool = left.toBoolean()
        val rightBool = right.toBoolean()
        return XdmAtomic(XsdBoolean.Companion(invoke(leftBool, rightBool)))
    }
}
