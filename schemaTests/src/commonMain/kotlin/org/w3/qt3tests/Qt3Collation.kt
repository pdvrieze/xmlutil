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

import io.github.pdvrieze.xml.schematypes.values.XsdAnyURI
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * An element which defines a collation URI used in the query.
 *     
 * The `uri` attribute is the collation URI as it actually appears in the XPath expression. There
 * is a small enumerated set of collation URIs that may appear in tests; these have a meaning that
 * is defined in the test suite. In addition, from 3.1 onwards, URIs in the Unicode Collation
 * Algorithm family can be used. If the implementation cannot bind arbitrary URIs to collations,
 * it may substitute this URI in the source expression by a different one having the same
 * semantics. If the implementation does not support the semantics of the collation, then it
 * should not run the test (support for collations other than the codepoint collation is not
 * a conformance requirement.
 * 
 * @property default The value `default="true"` indicates that this collation is to be the default collation
 *                         in the static context.
 */
@Serializable
@XmlSerialName("collation", QT3TNS)
class Qt3Collation(
    val uri: XsdAnyURI,
    val default: Boolean = false
) : Qt3Environment.Element {
/*
    enum class Collations(val uri: VAnyURI) {
        @SerialName("http://www.w3.org/2005/xpath-functions/collation/codepoint")
        CODEPOINT(VAnyURI("http://www.w3.org/2005/xpath-functions/collation/codepoint")),

        @SerialName("http://www.w3.org/2010/09/qt-fots-catalog/collation/caseblind")
        CASEBLIND(VAnyURI("http://www.w3.org/2010/09/qt-fots-catalog/collation/caseblind")),

        @SerialName("http://www.w3.org/2005/xpath-functions/collation/html-ascii-case-insensitive")
        ASCII_CASE_INSENSITIVE(VAnyURI("http://www.w3.org/2005/xpath-functions/collation/html-ascii-case-insensitive")),
    }
*/
}

