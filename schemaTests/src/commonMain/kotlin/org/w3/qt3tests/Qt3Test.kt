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

package org.w3.qt3tests

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xpath.XPathExpression
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import org.w3.qt3tests.context.AssertionResolutionContext
import org.w3.qt3tests.resolved.ResolvedQt3Test

/**
 * The content of the element is an XPath or XQuery expression to be evaluated.     
 * 
 * As an alternative to providing the content inline, it may be provided in an external
 * file referenced using the `file` attribute. This is done only exceptionally, where (a)
 * the query is unusually large, or (b) there is a need to test features that can only
 * be achieved with an external file, for example special encodings.
 */
@Serializable
@XmlSerialName("test", QT3TNS)
class Qt3Test(
    val file: VAnyURI? = null,
    @XmlValue val value: String
) {
    context(ctx: AssertionResolutionContext)
    fun resolve(): ResolvedQt3Test {
        val expr = when {
            value.isEmpty() -> null
            else -> runCatching { XPathExpression(value, ctx.namespaceContext, ctx.version) }.getOrNull()
        }
        return ResolvedQt3Test(file, expr)
    }
}
