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

        val normExpected = when {
            normalizeSpace -> xmlCollapseWhitespace(expected)
            else -> expected
        }

        val atomicVal = (r as? XdmAtomic<*>)?.value

        if (atomicVal != null && atomicVal !is XsdString) {
            val expectedNonString = atomicVal.schemaType.fromString(normExpected)
            if(atomicVal == expectedNonString) return AssertionResult.Success
        }

        val stringValue = context(ExprEvalContext.DUMMY) {
            StringFunctions.fnStringJoin(r, XdmAtomic(XsdString(" "))).value.xmlString.let {
                if (normalizeSpace) xmlCollapseWhitespace(it) else it
            }
        }
        if (normExpected == stringValue) return AssertionResult.Success

        return AssertionResult.Failure("Expected '$normExpected', got '$stringValue'", AssertionError("Assertion failure"))
    }

}
