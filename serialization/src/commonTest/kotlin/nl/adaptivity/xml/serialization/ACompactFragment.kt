/*
 * Copyright (c) 2021.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package nl.adaptivity.xml.serialization

import nl.adaptivity.xmlutil.XmlEvent
import nl.adaptivity.xmlutil.serialization.serializer
import nl.adaptivity.xmlutil.util.CompactFragment

class ACompactFragment : TestBase<CompactFragment>(
    CompactFragment(listOf(XmlEvent.NamespaceImpl("p", "urn:ns")), "<p:a>someA</p:a><b>someB</b>"),
    CompactFragment.serializer()
                                                  ) {
    override val expectedXML: String =
        "<compactFragment xmlns:p=\"urn:ns\"><p:a>someA</p:a><b>someB</b></compactFragment>"
    override val expectedJson: String =
        "{\"namespaces\":[{\"prefix\":\"p\",\"namespaceURI\":\"urn:ns\"}],\"content\":\"<p:a>someA</p:a><b>someB</b>\"}"
}