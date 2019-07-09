/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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
@file:ContextualSerialization(ChildA::class, ChildB::class)

package nl.adaptivity.xml.serialization

import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.serialization.XmlPolyChildren
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
open class Base

val baseModule = SerializersModule {
    polymorphic(Base::class) {
        ChildA::class with ChildA.serializer()
        ChildB::class with ChildB.serializer()
    }
}

@Serializable
@XmlSerialName("childA", namespace = "", prefix = "")
data class ChildA(val valueA: String) : Base()

@Serializable
@XmlSerialName("childB", namespace = "", prefix = "")
data class ChildB(val valueB: String) : Base()

@Serializable
data class Container(val label: String, @Polymorphic val member: Base)

@Serializable
data class Container2(val name: String, @XmlPolyChildren(["ChildA", "ChildB=better"]) val children: List<@Polymorphic Base>)

@SerialName("container-3")
@Serializable
data class Container3(val xxx: String, @SerialName("member") val members: List<@Polymorphic Base>)

@Serializable
sealed /*open*/ class SealedParent

val sealedModule = SerializersModule {
    polymorphic(SealedParent::class) {
        SealedA::class with SealedA.serializer()
        SealedB::class with SealedB.serializer()
    }
}

@Serializable
data class SealedA(val data: String, val extra: String = "2") : SealedParent()

@Serializable
data class SealedB(val main: String, val ext: Float = 0.5F) : SealedParent()

@Serializable
data class Sealed(val name: String, val members: List<@Polymorphic SealedParent>)

@Serializable
data class SealedSingle(val name: String, val member: SealedA)
