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

@file:UseSerializers(QNameSerializer::class)

package nl.adaptivity.xml.serialization

import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QNameSerializer

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

class QNameContentContainerTest : TestBase<QNameContentContainerTest.Container>(
    Container(
        "ABC Corp",
        QName("urn:foo", "bar", "baz"),
        QName("urn:example.org/2", "MyValue", "ns1"),
             ),
    Container.serializer()
                                                                               ) {
    override val expectedXML: String =
        "<container xmlns=\"urn:example.org\" xmlns:baz=\"urn:foo\" xmlns:ns1=\"urn:example.org/2\" param=\"ABC Corp\" name=\"baz:bar\"><value>ns1:MyValue</value></container>"
    override val expectedJson: String =
        "{\"param\":\"ABC Corp\",\"name\":{\"namespace\":\"urn:foo\",\"localPart\":\"bar\",\"prefix\":\"baz\"},\"value\":{\"namespace\":\"urn:example.org/2\",\"localPart\":\"MyValue\",\"prefix\":\"ns1\"}}"

    enum class AddresStatus { VALID, INVALID, TEMPORARY }

    @Serializable
    @XmlSerialName("container", namespace = "urn:example.org", prefix = "")
    data class Container(
        val param: String,
        @XmlElement(false)
        @XmlSerialName("name", namespace = "urn:example.org", prefix = "ns2")
        val name: QName,
        @XmlElement(true)
        @XmlSerialName("value", namespace = "urn:example.org", prefix = "ns2")
        val value: QName
                      )

}
