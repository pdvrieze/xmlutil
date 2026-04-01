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

import io.github.pdvrieze.formats.xpath.eval.data.XdmValue

class ResolvedQt3AnyOf(val assertions: List<ResolvedQt3Assertion>): ResolvedQt3Assertion() {
    override fun expectedErrors(accumulator: MutableList<ResolvedQt3AssertError>) {
        for (a in assertions) a.expectedErrors(accumulator)
    }

    override fun verify(evalResult: Result<io.github.pdvrieze.formats.xpath.eval.data.XdmValue>): AssertionResult {
        val failures = assertions
            .mapNotNull { it.verify(evalResult) as? AssertionResult.Failure }
        return when {
            failures.size== assertions.size -> AssertionResult.Failure("no passing assertion ${failures.joinToString { it.error }}")
            else -> AssertionResult.Success
        }
    }
}
