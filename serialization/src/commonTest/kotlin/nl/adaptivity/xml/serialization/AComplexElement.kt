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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlChildrenName
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

class AComplexElement : TestBase<AComplexElement.Special>(
    Special(),
    Special.serializer()
                                                         ) {
    override val expectedXML: String =
        """<localname xmlns="urn:namespace" paramA="valA"><paramb xmlns="urn:ns2">1</paramb><flags xmlns:f="urn:flag">""" +
                "<f:flag>2</f:flag>" +
                "<f:flag>3</f:flag>" +
                "<f:flag>4</f:flag>" +
                "<f:flag>5</f:flag>" +
                "<f:flag>6</f:flag>" +
                "</flags></localname>"
    override val expectedJson: String = "{\"paramA\":\"valA\",\"paramB\":1,\"flagValues\":[2,3,4,5,6]}"


    @Serializable
    @XmlSerialName("localname", "urn:namespace", prefix = "")
    data class Special(
        val paramA: String = "valA",
        @XmlSerialName("paramb", namespace = "urn:ns2", prefix = "")
        @XmlElement(true) val paramB: Int = 1,
        @SerialName("flagValues")
        @XmlSerialName("flags", namespace = "urn:namespace", prefix = "")
        @XmlChildrenName("flag", namespace = "urn:flag", prefix = "f")
        val param: List<Int> = listOf(2, 3, 4, 5, 6)
                      )

}