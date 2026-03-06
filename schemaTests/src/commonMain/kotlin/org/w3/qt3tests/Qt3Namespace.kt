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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * When this element is present in an environment, queries using this environment must have a
 * namespace binding in the static context that binds the specified prefix to the specified
 * namespace URI.
 *
 * A zero-length prefix denotes the default namespace for elements and types.
 *
 * All tests using an environment that contains a <code>namespace</code> element will be simple
 * XPath expressions, so that the test can be run either under XPath or XQuery. If the test is run
 * using an XPath processor, the namespace must be declared externally using the processor's API.
 * If it is run using an XQuery processor, the namespace can either be declared externally using
 * the processor's API, or a "declare namespace" declaration can be added to the query prolog.
 * Because the test is guaranteed to be a simple XPath expression, adding the namespace declaration
 * at the start is straightforward.
 *
 * Test Catalog005 checks that no test that requires XQuery uses an environment that contains a
 * namespace element.
 */
@Serializable
@XmlSerialName("namespace", QT3TNS)
class Qt3Namespace(
    override val prefix: String,
    @SerialName("uri") val uri: XsdAnyURI,
): Qt3Environment.Element, Namespace {
    override val namespaceURI: String get() = uri.value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Qt3Namespace

        if (prefix != other.prefix) return false
        if (uri != other.uri) return false

        return true
    }

    override fun hashCode(): Int {
        var result = prefix.hashCode()
        result = 31 * result + uri.hashCode()
        return result
    }

    override fun toString(): String {
        return buildString {
            append("Qt3Namespace(prefix='")
            append(prefix)
            append("', uri=")
            append(uri)
            append(')')
        }
    }


}
