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

import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal

@OptIn(XPathInternal::class)
class XdmError(
    val errorCode: EvaluationException.ErrorCodes,
    val message: String = errorCode.message,
    val cause: Throwable? = null
) : XdmValue() {

    constructor(errorCode: String, message: String) : this(EvaluationException.ErrorCodes.entries.first { it.code == errorCode }, message)

    constructor(errorCode: String) : this(EvaluationException.ErrorCodes.entries.first { it.code == errorCode })

    override fun get(index: Int): XdmValue {
        if (index != 0) throw IndexOutOfBoundsException()
        return this
    }

    context(ctx: ExprEvalContext)
    override fun atomize(): XdmError {
        return this
    }

    context(ctx: ExprEvalContext)
    override fun withType(type: XdmType): XdmValue {
        if (type !is XdmSequenceType.Error) throw IllegalArgumentException("Cannot cast to $type")
        return this
    }

    override fun isValEqual(expected: XdmValue): Boolean {
        return expected is XdmError && errorCode == expected.errorCode
    }

    override val type: XdmType = XdmSequenceType.Error()
}
