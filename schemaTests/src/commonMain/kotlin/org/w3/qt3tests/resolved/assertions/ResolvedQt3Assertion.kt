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

package org.w3.qt3tests.resolved.assertions

import io.github.pdvrieze.formats.xpath.data.XdmError
import io.github.pdvrieze.formats.xpath.data.XdmValue

abstract class ResolvedQt3Assertion {
    fun expectedErrors(): List<ResolvedQt3AssertError> = buildList {
        expectedErrors(this)
    }

    internal open fun expectedErrors(accumulator: MutableList<ResolvedQt3AssertError>) {}

    abstract fun verify(evalResult: XdmValue): AssertionResult

}

sealed class AssertionResult {
    object Success: AssertionResult()
    class Failure(val error: String) : AssertionResult() {
        constructor(error: XdmError): this(error.message)

        override fun toString(): String = "Failure('$error')"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Failure

            return error == other.error
        }

        override fun hashCode(): Int {
            return error.hashCode()
        }

    }
}
