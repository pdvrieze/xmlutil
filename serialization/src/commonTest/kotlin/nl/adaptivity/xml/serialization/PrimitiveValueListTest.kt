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
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import kotlin.test.Test
import kotlin.test.assertEquals

class PrimitiveValueListTest : PlatformTestBase<PrimitiveValueListTest.SimpleList>(
    SimpleList(1.1, 2.2, 3.3),
    SimpleList.serializer()
) {
    override val expectedXML: String = "<l>1.1 2.2 3.3</l>"
    override val expectedJson: String = "{\"values\":[1.1,2.2,3.3]}"

    @Serializable
    @SerialName("l")
    data class SimpleList(
        @XmlValue val values: List<Double>
    ) {
        constructor(vararg values: Double) : this(values.toList())
    }

}
