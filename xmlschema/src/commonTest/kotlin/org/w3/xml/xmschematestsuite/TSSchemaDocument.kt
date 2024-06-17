/*
 * Copyright (c) 2023.
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

package org.w3.xml.xmschematestsuite

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("schemaDocument", TS_NAMESPACE, TS_PREFIX)
class TSSchemaDocument(
    @XmlSerialName("href", XMLConstants.XLINK_NAMESPACE, "xlink")
    override val href: String,
    @XmlSerialName("type", XMLConstants.XLINK_NAMESPACE, "xlink")
    override val locator: String = "locator",
    @XmlSerialName("role", "", "")
    @XmlElement(false)
    val role: Role? = null,
    @XmlOtherAttributes
    override val otherAttributes: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
) : TSRefT {
    @Serializable
    enum class Role {
        @SerialName("principal") PRINCIPAL,

        @SerialName("imported") IMPORTED,

        @SerialName("included") INCLUDED,

        @SerialName("redefined") REDEFINED,

        @SerialName("overridden") OVERRIDDEN,

    }
}
