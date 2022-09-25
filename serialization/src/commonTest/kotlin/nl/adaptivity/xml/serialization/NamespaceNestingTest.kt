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
import nl.adaptivity.xmlutil.serialization.XmlValue
import kotlin.test.Test
import kotlin.test.assertEquals

/** Class from #99 (by dannyvelntesonos) */
class NamespaceNestingTest : PlatformTestBase<NamespaceNestingTest.A>(
    A("a", B("b", C("c"))),
    A.serializer()
) {
    override val expectedXML: String = "<a:ElementA xmlns:a=\"namespace/a\" value=\"a\"><ElementB xmlns=\"namespace/b\" value=\"b\"><ElementC xmlns=\"\" value=\"c\"/></ElementB></a:ElementA>"
    override val expectedJson: String = "{\"value\":\"a\",\"b\":{\"value\":\"b\",\"c\":{\"value\":\"c\"}}}"

    @Serializable
    @XmlSerialName("ElementA", "namespace/a", "a")
    data class A(val value:String, val b: B)

    @Serializable
    @XmlSerialName("ElementB", "namespace/b", "")
    data class B(val value:String, val c: C)

    @Serializable
    @XmlSerialName("ElementC", "", "")
    data class C(
        val value:String
    )


}
