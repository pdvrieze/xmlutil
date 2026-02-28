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

package org.w3.qt3tests.assertions

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import org.w3.qt3tests.QT3TNS
import org.w3.qt3tests.UnresolvedXPathExpr
import org.w3.qt3tests.context.AssertionResolutionContext
import org.w3.qt3tests.resolved.assertions.ResolvedQt3AssertDeepEq

/**
 * Asserts that the result must be a sequence of atomic values that is deep-equal
 * to the supplied sequence under the rules of the deep-equal() function.
 */
@Serializable
@XmlSerialName("assert-deep-eq", QT3TNS)
class Qt3AssertDeepEq(@XmlValue val assertion: UnresolvedXPathExpr): Qt3AbstractAssertion() {
    context(ctx: AssertionResolutionContext)
    override fun resolve(): ResolvedQt3AssertDeepEq {
        return ResolvedQt3AssertDeepEq(assertion.resolve().getOrThrow())
    }
}

