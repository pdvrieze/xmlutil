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

import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.functions.impl.StringFunctions
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.values.XsdString
import nl.adaptivity.xmlutil.xmlCollapseWhitespace
import org.w3.qt3tests.resolved.ResolvedQt3TestCase

@OptIn(XPathInternal::class)
class ResolvedQt3AssertStringValue(val expected: String, val normalizeSpace: Boolean) : ResolvedQt3Assertion() {
    override fun verify(evalResult: Result<XdmValue<*>>, testCase: ResolvedQt3TestCase): AssertionResult {
        val r = evalResult.getOrElse { return AssertionResult.Failure(it) }
        var stringValue = context(ExprEvalContext.DUMMY) {
            StringFunctions.fnStringJoin(r, XdmAtomic(XsdString(" "))).value.xmlString
        }
        val normExpected: String
        if (normalizeSpace) {
            normExpected = xmlCollapseWhitespace(expected)
            stringValue = xmlCollapseWhitespace(stringValue)
        } else {
            normExpected = expected
        }


        return when {
            normExpected == stringValue -> AssertionResult.Success
            else -> AssertionResult.Failure("Expected '$normExpected', got '$stringValue'", AssertionError("Assertion failure"))
        }
    }

}
