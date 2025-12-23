/*
 * Copyright (c) 2024-2025.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

@file:MustUseReturnValues

package nl.adaptivity.xml.serialization.regressions

import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xml.serialization.pedantic
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML1_0
import nl.adaptivity.xmlutil.serialization.XmlValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ContextualAndValue238 {

    val xml
        get() = XML1_0.pedantic(
            serializersModule = SerializersModule {
                this.contextual(Uuid::class, Uuid.serializer())
            }
        ) {
            xmlDeclMode = XmlDeclMode.None
        }

    @Test
    fun testSerializeValueAndContextualNull() {
        val actual = xml.encodeToString(Foo.serializer(), Foo("bar"))
        assertEquals("<Foo>bar</Foo>", actual)
    }

    @Test
    fun testSerializeValueAndPseudoUuid() {
        val actual = xml.encodeToString(Bar.serializer(), Bar("foo"))
        assertEquals("<Bar>foo</Bar>", actual)
    }

    @Serializable
    data class Foo(
        @XmlValue
        val foo: String,

        val id: @Contextual Uuid? = null,
    )

    @Serializable
    data class Bar(
        @XmlValue
        val bar: String,
        val uuid: @Serializable(PseudoUuidSerializer::class) Uuid? = null,
    )

}

@OptIn(ExperimentalUuidApi::class)
object PseudoUuidSerializer: KSerializer<Uuid> by Uuid.serializer()
