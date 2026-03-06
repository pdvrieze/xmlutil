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

import io.github.pdvrieze.xml.schematypes.values.XsdAnyURI
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import org.w3.qt3tests.QT3TNS
import org.w3.qt3tests.attrGroups.Qt3FileAttr
import org.w3.qt3tests.context.AssertionResolutionContext
import org.w3.qt3tests.resolved.assertions.ResolvedQt3Assertion
import org.w3.qt3tests.resolved.assertions.ResolvedQt3SerializationMatches

@Serializable
@XmlSerialName("serialization-matches", QT3TNS)
class Qt3SerializationMatches(
    @XmlValue val assertion: String,
    override val file: XsdAnyURI? = null,
    val flags: String? = null,
): Qt3AbstractAssertion(), Qt3FileAttr {
    context(ctx: AssertionResolutionContext)
    override fun resolve(): ResolvedQt3Assertion {
        return ResolvedQt3SerializationMatches(assertion, file, flags)
    }
}

