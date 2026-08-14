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
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmSchemaType
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.values.XsdBoolean
import org.w3.qt3tests.context.Qt3NsContext
import org.w3.qt3tests.resolved.ResolvedQt3TestCase

@OptIn(XPathInternal::class)
class ResolvedQt3AssertType(val type: String): ResolvedQt3Assertion() {
    override fun verify(evalResult: Result<XdmValue<*>>, testCase: ResolvedQt3TestCase): AssertionResult {
        val evalResult = evalResult.getOrElse { return AssertionResult.Failure(it) }

        val typeExpr = XPathExpression(". instance of $type", Qt3NsContext)

        val r = typeExpr.eval(evalResult).single()
        if (r !is XdmAtomic<*>) return AssertionResult.Failure("Expected atomic value, got $r", AssertionError("Assertion failure"))
        val v = r.value
        if (v !is XsdBoolean) return AssertionResult.Failure("Expected boolean value, got $v", AssertionError("Assertion failure"))

        if (v.value) return AssertionResult.Success

        val actualType = evalResult.staticType as? XdmSchemaType ?: return AssertionResult.Failure("Expected type ($typeExpr), got ${evalResult.staticType}", AssertionError("Assertion failure"))


        return AssertionResult.Failure("Expected type ($typeExpr), got ${actualType.schemaType}", AssertionError("Assertion failure"))
    }

}
