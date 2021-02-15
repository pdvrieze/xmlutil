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

class ListTest : TestBase<ListTest.SimpleList>(
    SimpleList("1", "2", "3"),
    SimpleList.serializer()
                                              ) {
    override val expectedXML: String = "<l><values>1</values><values>2</values><values>3</values></l>"
    override val expectedJson: String = "{\"values\":[\"1\",\"2\",\"3\"]}"

    @Serializable
    @SerialName("l")
    data class SimpleList(val values: List<String>) {
        constructor(vararg values: String) : this(values.toList())
    }

}