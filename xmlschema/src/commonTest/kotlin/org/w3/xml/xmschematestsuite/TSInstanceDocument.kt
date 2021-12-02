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

package org.w3.xml.xmschematestsuite

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("instanceDocument", TS_NAMESPACE, TS_PREFIX)
class TSInstanceDocument(
    @XmlSerialName("href", XmlSchemaConstants.XLINK_NAMESPACE, "xlink")
    override val href: String,
    @XmlSerialName("type", XmlSchemaConstants.XLINK_NAMESPACE, "xlink")
    override val locator: String = "locator",
    @XmlOtherAttributes
    override val otherAttributes: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
) : TSRefT
