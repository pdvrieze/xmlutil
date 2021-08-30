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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.serialization.XmlSerialName

class CustomSerializedClass : TestBase<CustomSerializedClass.CustomContainer>(
    CustomContainer(Custom("foobar")),
    CustomContainer.serializer()
) {

    override val expectedXML: String = "<CustomContainer elem=\"foobar\"/>"
    override val expectedJson: String = "{\"nonXmlElemName\":\"foobar\"}"

    @Serializable
    data class CustomContainer(
        @SerialName("nonXmlElemName")
        @XmlSerialName("elem")
        @Serializable(with = CustomSerializer::class)
        val somethingElse: Custom
    )

    data class Custom(val property: String)

    @Serializer(forClass = Custom::class)
    class CustomSerializer : KSerializer<Custom> {

        override val descriptor: SerialDescriptor = serialDescriptor<String>()


        override fun deserialize(decoder: Decoder): Custom {
            return Custom(decoder.decodeString())
        }

        override fun serialize(encoder: Encoder, value: Custom) {
            encoder.encodeString(value.property)
        }
    }

}
