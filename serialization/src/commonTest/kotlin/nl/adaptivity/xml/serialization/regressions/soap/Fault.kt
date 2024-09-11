/*
 * Copyright (c) 2024.
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

package nl.adaptivity.xml.serialization.regressions.soap

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("Fault", Envelope.NAMESPACE, Envelope.PREFIX)
class Fault(
    @XmlSerialName("Code", Envelope.NAMESPACE)
    val code: Element,
    @XmlSerialName("Reason", Envelope.NAMESPACE)
    val reason: Element,
    @XmlSerialName("Node", Envelope.NAMESPACE)
    val node: Element? = null,
    @XmlSerialName("Role", Envelope.NAMESPACE)
    val role: Element? = null,
    @XmlSerialName("Detail", Envelope.NAMESPACE)
    val detail: Element? = null,
) {
}
