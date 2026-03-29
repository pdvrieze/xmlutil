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

import io.github.pdvrieze.formats.xpath.data.XdmSchemaType
import io.github.pdvrieze.formats.xpath.data.XdmValue
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.builtinType
import nl.adaptivity.xmlutil.XMLConstants

@OptIn(XPathInternal::class)
class ResolvedQt3AssertType(val type: String): ResolvedQt3Assertion() {
    override fun verify(evalResult: Result<XdmValue>): AssertionResult {
        val evalResult = evalResult.getOrElse { return AssertionResult.Failure(it) }

        val expectedType = builtinType(type.substringAfterLast(':'), XMLConstants.XSD_NS_URI)
            ?: return AssertionResult.Failure("Unknown type $type")

        val actualType = evalResult.type as? XdmSchemaType ?: return AssertionResult.Failure("Expected Schema type $expectedType, got ${evalResult.type}")

        if (actualType.schemaType.derivesFrom(expectedType)) return AssertionResult.Success

        return AssertionResult.Failure("Expected Schema type $expectedType, got ${actualType.schemaType}")
    }

}
