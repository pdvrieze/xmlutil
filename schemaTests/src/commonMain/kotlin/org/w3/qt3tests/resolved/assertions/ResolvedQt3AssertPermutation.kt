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

import io.github.pdvrieze.formats.xpath.XPathExpression
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import org.w3.qt3tests.resolved.ResolvedQt3TestCase

@OptIn(XPathInternal::class)
class ResolvedQt3AssertPermutation(val xPathExpression: XPathExpression) : ResolvedQt3Assertion() {
    override fun verify(evalResult: Result<XdmValue<*>>, testCase: ResolvedQt3TestCase): AssertionResult {
        val evalResult = evalResult.getOrElse { return AssertionResult.Failure(it) }
        val expected = xPathExpression.eval(evalResult).toMutableSet()

        if (evalResult.size != expected.size) return AssertionResult.Failure(
            "Permutation needs equal item count ${expected.size} items, got ${evalResult.size}",
            AssertionError("Assertion failure")
        )
        context(ExprEvalContext.DUMMY) {
            for (item in evalResult) {
                val found = expected.firstOrNull { it.isDeepEqual(item, null) }
                if (found == null) return AssertionResult.Failure(
                    "Permutation needs equal item count ${expected.size} items, got ${evalResult.size}",
                    AssertionError("Assertion failure")
                )
                if (!expected.remove(found)) return AssertionResult.Failure(
                    "Deep equal does not match equal/hashcode",
                    AssertionError("Assertion failure")
                )
            }
        }
        return AssertionResult.Success
    }
}
