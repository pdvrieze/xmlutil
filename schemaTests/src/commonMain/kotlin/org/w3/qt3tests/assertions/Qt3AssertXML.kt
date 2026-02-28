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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import org.w3.qt3tests.QT3TNS
import org.w3.qt3tests.attrGroups.Qt3FileAttr
import org.w3.qt3tests.context.AssertionResolutionContext
import org.w3.qt3tests.resolved.assertions.ResolvedQt3AssertXML

/**
 * Asserts the result of the query by providing a serialization of the expression
 * result using the default serialization parameters method="xml" indent="no"
 * omit-xml-declaration="yes".
 *
 * Previously called assert-serialization.
 * Note that this assertion is not used to test serialization; it is used
 * as a way of supplying the expected results of the query in the form of an XML
 * document.
 *
 * The assertion is true if the result of parsing and canonicalizing the XML
 * given in the body of the assert-xml element is the same (byte-for-byte) as
 * the result of canonicalizing the XML result of the query. As an alternative
 * to canonicalizing, the results may be compared using the fn:deep-equal()
 * function.
 *
 * The value will not necessarily be a well-formed document (it may be a fragment).
 * The comparison can be done by converting the string into a well-formed
 * document by adding a wrapper element.
 *
 * @property ignorePrefixes If this attribute is present with the value "`true`", it indicates
 * that the serialized result contains system-generated prefixes which can lead to ignorable
 * differences between the actual result and the serialized result.
 *
 * This attribute is rarely used, and should be avoided for new tests. Instead, the test result
 * should be expressed using assertions that take no account of the namespace prefixes generated.
 */
@Serializable
@XmlSerialName("assert-xml", QT3TNS)
class Qt3AssertXML(
    @XmlValue val assertion: String,
    override val file: VAnyURI? = null,
    @SerialName("ignore-prefixes")
    val ignorePrefixes: Boolean = false
): Qt3AbstractAssertion(), Qt3FileAttr {
    context(ctx: AssertionResolutionContext)
    override fun resolve(): ResolvedQt3AssertXML {
        return ResolvedQt3AssertXML(assertion, file, ignorePrefixes)
    }
}

