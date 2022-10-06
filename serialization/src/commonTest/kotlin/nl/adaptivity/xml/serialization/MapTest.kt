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

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement

/** Test for #102 */
class MapTest : PlatformTestBase<MapTest.ListContainer>(
    ListContainer(
        listOf(
            MapContainer(id="myId", map = mapOf(
                "a" to MapElement("valueOfA"),
                "b" to MapElement("valueOfB")
            ))
        )
    ),
    ListContainer.serializer()
) {
    override val expectedXML: String =
        "<Business name=\"ABC Corp\"><headOffice houseNumber=\"1\" street=\"ABC road\" city=\"ABCVille\" status=\"VALID\"/></Business>"
    override val expectedJson: String =
        "{\"values\":[{\"id\":\"myId\",\"map\":{\"a\":{\"name\":\"valueOfA\"},\"b\":{\"name\":\"valueOfB\"}}}]}"

    enum class AddresStatus { VALID, INVALID, TEMPORARY }

    @Serializable
    data class ListContainer(val values: List<MapContainer>)

    @Serializable
    data class MapContainer(
        val id: String,
        @XmlElement(true)
        val map: Map<String, MapElement> = mapOf(),
    )

    @Serializable
    data class MapElement(val name: String)

}
