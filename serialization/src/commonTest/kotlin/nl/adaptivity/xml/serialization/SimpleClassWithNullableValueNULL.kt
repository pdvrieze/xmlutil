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
import nl.adaptivity.xmlutil.serialization.XmlSerialName

class SimpleClassWithNullableValueNULL : PlatformTestBase<SimpleClassWithNullableValueNULL.NullableContainer>(
    NullableContainer(),
    NullableContainer.serializer()
) {
    override val expectedXML: String = "<p:NullableContainer xmlns:p=\"urn:myurn\"/>"
    override val expectedJson: String = "{\"bar\":null}"

    @Serializable
    @XmlSerialName("NullableContainer", "urn:myurn", "p")
    data class NullableContainer(var bar: String? = null)

}
