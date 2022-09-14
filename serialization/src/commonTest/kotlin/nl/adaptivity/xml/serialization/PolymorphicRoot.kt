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

import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * Polymorphic serialization should also work for root class.
 */
class PolymorphicRoot : PlatformTestPolymorphicBase<PolymorphicRoot.Base>(
    ChildA("data"),
    PolymorphicSerializer(Base::class),
    baseModule
) {
    override val expectedXML: String
        get() = "<childA valueA=\"data\"/>"
    override val expectedJson: String
        get() = "{\"type\":\"nl.adaptivity.xml.serialization.PolymorphicRoot.ChildA\",\"valueA\":\"data\"}"
    override val expectedNonAutoPolymorphicXML: String
        get() = "<Base type=\".ChildA\"><value valueA=\"data\"/></Base>"

    override val expectedXSIPolymorphicXML: String
        get() = "<Base xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"childA\" valueA=\"data\"/>"

    @Serializable
    open class Base

    @Serializable
    @XmlSerialName("childA", "", "")
    data class ChildA(val valueA: String) : Base()

    @Serializable
    @SerialName("childBNameFromAnnotation")
    @XmlSerialName("childB", "", "")
    data class ChildB(val a: Int, val b: Int, val c: Int, val valueB: String) : Base()

    companion object {

        private val baseModule = SerializersModule {
            polymorphic(Base::class) {
                subclass(ChildA::class)
                subclass(ChildB::class)
            }
        }

    }

}
