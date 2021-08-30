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
import nl.adaptivity.xmlutil.serialization.XmlElement

class NullableListTestWithElements : TestBase<NullableListTestWithElements.NullList>(
    NullList(
        "A String", listOf(
            NullListElement("Another String1"),
            NullListElement("Another String2")
                          )
            ),
    NullList.serializer()
                                                                                    ) {
    override val expectedXML: String
        get() = "<Baz><Str>A String</Str><Bar><AnotherStr>Another String1</AnotherStr></Bar><Bar><AnotherStr>Another String2</AnotherStr></Bar></Baz>"
    override val expectedJson: String
        get() = "{\"Str\":\"A String\",\"Bar\":[{\"AnotherStr\":\"Another String1\"},{\"AnotherStr\":\"Another String2\"}]}"

    @Serializable
    @SerialName("Bar")
    data class NullListElement(
        @XmlElement
        @SerialName("AnotherStr")
        val anotherString: String
    )

    @Serializable
    @SerialName("Baz")
    data class NullList(
        @XmlElement
        @SerialName("Str")
        val aString: String,

        @XmlElement
        @SerialName("Bar")
        val aList: List<NullListElement>? = null
    )

}
