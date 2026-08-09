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

import io.github.pdvrieze.formats.xpath.eval.EvaluationException

sealed class AssertionResult {
    object Success: AssertionResult() {
        override fun toString(): String = "Success"
    }

    class Failure(val error: String, val cause: Throwable/*? = null*/) : AssertionResult() {
        constructor(error: EvaluationException): this(error.message ?: error.errorCode?.message ?: "Unknown error", error)
        constructor(error: Throwable): this(error.message ?: "Unknown error", error)

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

inline fun AssertionResult.flatMap(onSuccess: () -> AssertionResult): AssertionResult {
    return when (this) {
        is AssertionResult.Failure -> this
        is AssertionResult.Success -> onSuccess()
    }
}

@IgnorableReturnValue
inline fun AssertionResult.onFailure(action: (AssertionResult.Failure) -> Nothing): AssertionResult.Success = when (this) {
    is AssertionResult.Success -> this
    is AssertionResult.Failure -> action(this)
}
