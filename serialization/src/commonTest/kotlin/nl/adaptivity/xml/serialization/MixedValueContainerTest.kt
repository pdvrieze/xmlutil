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

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

class MixedValueContainerTest : TestPolymorphicBase<MixedValueContainerTest.MixedValueContainer>(
    MixedValueContainer(listOf("foo", Address("10", "Downing Street", "London"), "bar")),
    MixedValueContainer.serializer(),
    MixedValueContainer.module(),
    baseJsonFormat = Json {
        useArrayPolymorphism = true
        serializersModule = MixedValueContainer.module()
        encodeDefaults = true
    }
                                                                        ) {
    override val expectedXML: String =
        "<MixedValueContainer>foo<address houseNumber=\"10\" street=\"Downing Street\" city=\"London\" status=\"VALID\"/>bar</MixedValueContainer>"
    override val expectedNonAutoPolymorphicXML: String =
        "<MixedValueContainer><data type=\"kotlin.String\"><value>foo</value></data><data type=\".Address\"><value houseNumber=\"10\" street=\"Downing Street\" city=\"London\" status=\"VALID\"/></data><data type=\"kotlin.String\"><value>bar</value></data></MixedValueContainer>"
    override val expectedJson: String =
        "{\"data\":[[\"kotlin.String\",\"foo\"],[\"nl.adaptivity.xml.serialization.MixedValueContainerTest.Address\",{\"houseNumber\":\"10\",\"street\":\"Downing Street\",\"city\":\"London\",\"status\":\"VALID\"}],[\"kotlin.String\",\"bar\"]]}"

    enum class AddresStatus { VALID, INVALID, TEMPORARY }

    @Serializable
    @XmlSerialName("address")
    data class Address(
        val houseNumber: String,
        val street: String,
        val city: String,
        @XmlElement(false) val status: AddresStatus = AddresStatus.VALID
    )

    @Serializable
    data class MixedValueContainer(@XmlValue(true) val data: List<@Polymorphic Any>) {
        companion object {
            fun module(): SerializersModule {
                return SerializersModule {
                    polymorphic(Any::class, String::class, String.serializer())
                    polymorphic(Any::class, Address::class, Address.serializer())
                }
            }
        }
    }

}
