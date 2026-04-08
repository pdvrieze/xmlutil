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
import io.github.pdvrieze.formats.xpath.impl.ContextItem
import io.github.pdvrieze.formats.xpath.impl.EvalContext
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import nl.adaptivity.xmlutil.SimpleNamespaceContext
import org.w3.qt3tests.resolved.ResolvedQt3TestCase

@OptIn(XPathInternal::class)
class ResolvedQt3Assert(val assertion: XPathExpression) : ResolvedQt3Assertion() {
    override fun verify(evalResult: Result<XdmValue<*>>, testCase: ResolvedQt3TestCase): AssertionResult {
        val evalResult = evalResult.getOrElse { return AssertionResult.Failure(it) }
        val nsContext = testCase.environment?.getOrThrow()?.getNsContext() ?: SimpleNamespaceContext()
        val vars = mapOf("" to mapOf(
            "result" to evalResult,
        ))

        val context = EvalContext(nsContext, ContextItem(evalResult, 1, 1), variables = vars)

        val assertionRaw = assertion.eval(evalResult, nsContext, vars)

        val assertResult = context(ExprEvalContext.DUMMY) {
            assertionRaw.toBoolean()
        }
        return when (assertResult) {
            true -> AssertionResult.Success
            else -> AssertionResult.Failure("assertion '${assertion.xmlString}' failed", AssertionError("Assertion failure"))
        }
    }
}
