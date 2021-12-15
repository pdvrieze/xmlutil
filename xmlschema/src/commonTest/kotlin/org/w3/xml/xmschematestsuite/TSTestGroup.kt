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

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("testGroup", TS_NAMESPACE, TS_PREFIX)
class TSTestGroup(
    @XmlElement(true)
    val annotation: TSAnnotation? = null,
    @XmlElement(true)
    val documentationReferences: List<TSDocumentationReference> = emptyList(),
    @XmlElement(true)
    val schemaTest: TSSchemaTest? = null,
    @XmlElement(true)
    val instanceTests: List<TSInstanceTest> = emptyList(),
    val name: String,
    @XmlOtherAttributes
    val otherAttributes: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
) {
    fun documentationString(): String {
        return buildString {
            if (annotation!=null) {
                for (elem in annotation.elements) {
                    when(elem) {
                        is TSAnnotation.Documentation -> append(elem.info.contentString)
                        else -> Unit
                    }
                }
            }
        }
    }
}
