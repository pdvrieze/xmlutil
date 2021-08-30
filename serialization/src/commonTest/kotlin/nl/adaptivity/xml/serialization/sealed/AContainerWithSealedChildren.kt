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

package nl.adaptivity.xml.serialization.sealed

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.EmptySerializersModule
import nl.adaptivity.xml.serialization.TestPolymorphicBase
import nl.adaptivity.xmlutil.serialization.XmlSerialName

class AContainerWithSealedChildren : TestPolymorphicBase<Sealed>(
    Sealed("mySealed", listOf(SealedA("a-data"), SealedB("b-data"))),
    Sealed.serializer(),
    EmptySerializersModule//sealedModule
) {
    override val expectedXML: String
        get() = "<Sealed name=\"mySealed\"><SealedA data=\"a-data\" extra=\"2\"/><SealedB_renamed main=\"b-data\" ext=\"0.5\"/></Sealed>"
    override val expectedJson: String
        get() = "{\"name\":\"mySealed\",\"members\":[{\"type\":\"nl.adaptivity.xml.serialization.sealed.SealedA\",\"data\":\"a-data\",\"extra\":\"2\"},{\"type\":\"nl.adaptivity.xml.serialization.sealed.SealedB\",\"main\":\"b-data\",\"ext\":0.5}]}"
    override val expectedNonAutoPolymorphicXML: String
        get() = "<Sealed name=\"mySealed\"><member type=\".SealedA\"><value data=\"a-data\" extra=\"2\"/></member><member type=\".SealedB\"><value main=\"b-data\" ext=\"0.5\"/></member></Sealed>"

}

@Serializable
data class Sealed(
    val name: String,
    @XmlSerialName("member")
    val members: List<SealedParent>
)
